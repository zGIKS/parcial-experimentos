package pe.entregago.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.entregago.application.ConfirmOrderCommandService;
import pe.entregago.application.ConfirmOrderResult;
import pe.entregago.domain.model.BusinessRuleViolation;
import pe.entregago.domain.model.aggregates.Merchant;
import pe.entregago.domain.model.aggregates.Order;
import pe.entregago.domain.model.commands.ConfirmOrderCommand;
import pe.entregago.domain.model.entities.Coupon;
import pe.entregago.domain.model.entities.CustomerAddress;
import pe.entregago.domain.model.entities.OrderLine;
import pe.entregago.domain.model.entities.Product;
import pe.entregago.domain.model.entities.StockReservation;
import pe.entregago.domain.model.enums.BusinessRuleCode;
import pe.entregago.domain.model.valueobjects.OrderId;
import pe.entregago.domain.model.valueobjects.PaymentAuthorization;
import pe.entregago.domain.model.valueobjects.PriceBreakdown;
import pe.entregago.domain.model.valueobjects.ReservationId;
import pe.entregago.domain.repositories.*;
import pe.entregago.domain.services.OrderPricingPolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConfirmOrderCommandServiceImpl implements ConfirmOrderCommandService {
    private final MerchantRepository merchants;
    private final CustomerAddressRepository addresses;
    private final ProductRepository products;
    private final CouponRepository coupons;
    private final StockReservationRepository reservations;
    private final PaymentGateway payments;
    private final OrderRepository orders;
    private final OrderPricingPolicy pricing;
    private final Clock clock;

    public ConfirmOrderCommandServiceImpl(MerchantRepository merchants, CustomerAddressRepository addresses,
            ProductRepository products, CouponRepository coupons, StockReservationRepository reservations,
            PaymentGateway payments, OrderRepository orders, OrderPricingPolicy pricing, Clock clock) {
        this.merchants=merchants; this.addresses=addresses; this.products=products; this.coupons=coupons;
        this.reservations=reservations; this.payments=payments; this.orders=orders; this.pricing=pricing; this.clock=clock;
    }

    @Override public ConfirmOrderResult handle(ConfirmOrderCommand command) {
        var now=clock.instant();
        Merchant merchant=merchants.findById(command.merchantId()).orElseThrow(() ->
                new BusinessRuleViolation(BusinessRuleCode.MERCHANT_NOT_FOUND, "merchant was not found"));
        if (!merchant.isOpenAt(now)) throw new BusinessRuleViolation(BusinessRuleCode.MERCHANT_CLOSED, "merchant is closed");
        CustomerAddress address=addresses.findById(command.addressId()).orElseThrow(() ->
                new BusinessRuleViolation(BusinessRuleCode.ADDRESS_NOT_FOUND, "delivery address was not found"));
        if (!address.customerId().equals(command.customerId()))
            throw new BusinessRuleViolation(BusinessRuleCode.ADDRESS_NOT_OWNED, "delivery address does not belong to the customer");
        if (!merchant.serves(address.zoneId()))
            throw new BusinessRuleViolation(BusinessRuleCode.ADDRESS_OUT_OF_COVERAGE, "delivery address is outside the merchant coverage");

        List<OrderLine> lines=new ArrayList<>();
        for (var requested : command.items()) {
            Product product=products.findById(requested.productId()).orElseThrow(() ->
                    new BusinessRuleViolation(BusinessRuleCode.PRODUCT_NOT_FOUND, "product was not found: " + requested.productId().value()));
            if (!product.merchantId().equals(merchant.id()))
                throw new BusinessRuleViolation(BusinessRuleCode.PRODUCT_FROM_ANOTHER_MERCHANT, "all products must belong to the selected merchant");
            if (!product.available()) throw new BusinessRuleViolation(BusinessRuleCode.PRODUCT_UNAVAILABLE, "product is unavailable: " + product.name());
            lines.add(new OrderLine(product.id(), product.name(), product.unitPrice(), requested.quantity(), requested.observations()));
        }

        Coupon coupon=command.couponCode() == null ? null : coupons.findByCode(command.couponCode()).orElse(null);
        PriceBreakdown quote=pricing.quote(merchant.id(), lines, coupon, LocalDate.now(clock));
        OrderId orderId=OrderId.generate();
        ReservationId reservationId=new ReservationId(UUID.randomUUID().toString());
        StockReservation reservation=reservations.reserveAll(reservationId, command.items(), now);
        PaymentAuthorization authorization=null;
        try {
            authorization=payments.authorize(command.paymentMethodId(), quote.total(), orderId);
            if (authorization == null || !authorization.approved())
                throw new BusinessRuleViolation(BusinessRuleCode.PAYMENT_NOT_AUTHORIZED, "payment was not authorized");
            Order order=new Order(orderId, command.customerId(), merchant.id(), address.id(), lines, quote, now);
            order.confirm(authorization, reservation, now);
            orders.save(order);
            if (quote.appliedCouponCode() != null) coupons.recordUse(command.couponCode());
            return new ConfirmOrderResult(orderId, order.status(), quote, lines, reservation.id(), authorization.reference());
        } catch (RuntimeException failure) {
            if (authorization != null && authorization.approved()) {
                try { payments.voidAuthorization(authorization); } catch (RuntimeException compensationFailure) { failure.addSuppressed(compensationFailure); }
            }
            try { reservations.release(reservationId); } catch (RuntimeException compensationFailure) { failure.addSuppressed(compensationFailure); }
            throw failure;
        }
    }
}
