package pe.entregago.application;

import org.springframework.stereotype.Service;
import pe.entregago.domain.model.AddressSnapshot;
import pe.entregago.domain.model.FailureReason;
import pe.entregago.domain.model.MerchantSnapshot;
import pe.entregago.domain.model.OrderConfirmationCommand;
import pe.entregago.domain.model.OrderConfirmationResult;
import pe.entregago.domain.model.OrderRejectedException;
import pe.entregago.domain.model.OrderStatus;
import pe.entregago.domain.model.PaymentAuthorization;
import pe.entregago.domain.model.PricingLine;
import pe.entregago.domain.model.ProductSnapshot;
import pe.entregago.domain.model.ReservationLine;
import pe.entregago.domain.model.ReservationToken;
import pe.entregago.domain.port.AddressRepository;
import pe.entregago.domain.port.CoveragePolicy;
import pe.entregago.domain.port.InventoryPort;
import pe.entregago.domain.port.MerchantRepository;
import pe.entregago.domain.port.OrderRepository;
import pe.entregago.domain.port.PaymentGateway;
import pe.entregago.domain.port.PricingService;
import pe.entregago.domain.port.ProductCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Coordinates the business rule; persistence, stock and payment stay behind ports. */
@Service
public class OrderConfirmationService {
    private final MerchantRepository merchants;
    private final AddressRepository addresses;
    private final ProductCatalog catalog;
    private final CoveragePolicy coverage;
    private final InventoryPort inventory;
    private final PricingService pricing;
    private final PaymentGateway payments;
    private final OrderRepository orders;

    public OrderConfirmationService(
            MerchantRepository merchants,
            AddressRepository addresses,
            ProductCatalog catalog,
            CoveragePolicy coverage,
            InventoryPort inventory,
            PricingService pricing,
            PaymentGateway payments,
            OrderRepository orders) {
        this.merchants = merchants;
        this.addresses = addresses;
        this.catalog = catalog;
        this.coverage = coverage;
        this.inventory = inventory;
        this.pricing = pricing;
        this.payments = payments;
        this.orders = orders;
    }

    public OrderConfirmationResult confirm(OrderConfirmationCommand command) {
        MerchantSnapshot merchant = merchants.findMerchant(command.merchantId())
                .orElseThrow(() -> new OrderRejectedException(FailureReason.MERCHANT_NOT_FOUND));
        if (!merchant.open()) {
            throw new OrderRejectedException(FailureReason.MERCHANT_CLOSED);
        }

        AddressSnapshot address = addresses.findAddress(command.addressId())
                .orElseThrow(() -> new OrderRejectedException(FailureReason.ADDRESS_NOT_FOUND));
        if (!address.customerId().equals(command.customerId())) {
            throw new OrderRejectedException(FailureReason.ADDRESS_NOT_OWNED);
        }
        if (!coverage.covers(merchant, address)) {
            throw new OrderRejectedException(FailureReason.OUT_OF_COVERAGE);
        }

        List<PricingLine> pricedLines = new ArrayList<>();
        List<ReservationLine> reservationLines = new ArrayList<>();
        for (var requested : command.items()) {
            ProductSnapshot product = catalog.findProduct(requested.productId())
                    .orElseThrow(() -> new OrderRejectedException(FailureReason.PRODUCT_NOT_FOUND));
            if (!product.merchantId().equals(command.merchantId()) || !product.available()) {
                throw new OrderRejectedException(FailureReason.PRODUCT_UNAVAILABLE);
            }
            if (requested.quantity() > product.availableStock()) {
                throw new OrderRejectedException(FailureReason.INSUFFICIENT_STOCK);
            }
            pricedLines.add(new PricingLine(product.id(), product.unitPrice(), requested.quantity()));
            reservationLines.add(new ReservationLine(product.id(), requested.quantity()));
        }

        var quote = pricing.quote(command.merchantId(), pricedLines, command.couponCode());
        ReservationToken reservation = inventory.reserveAll(reservationLines);
        PaymentAuthorization authorization = null;
        String orderId = UUID.randomUUID().toString();
        try {
            authorization = payments.authorize(command.paymentMethodId(), quote.total(), orderId);
            if (!authorization.approved()) {
                throw new OrderRejectedException(FailureReason.PAYMENT_NOT_AUTHORIZED);
            }
            var confirmed = new OrderConfirmationResult(
                    orderId,
                    command.customerId(),
                    command.merchantId(),
                    OrderStatus.CONFIRMED,
                    quote,
                    command.items(),
                    authorization.reference());
            orders.saveConfirmed(confirmed);
            return confirmed;
        } catch (RuntimeException failure) {
            try {
                inventory.release(reservation);
            } catch (RuntimeException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
            if (authorization != null && authorization.approved()) {
                try {
                    payments.voidAuthorization(authorization);
                } catch (RuntimeException voidFailure) {
                    failure.addSuppressed(voidFailure);
                }
            }
            throw failure;
        }
    }
}
