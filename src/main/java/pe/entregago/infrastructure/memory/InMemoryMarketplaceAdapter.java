package pe.entregago.infrastructure.memory;

import org.springframework.stereotype.Component;
import pe.entregago.domain.model.AddressSnapshot;
import pe.entregago.domain.model.CouponOffer;
import pe.entregago.domain.model.MerchantSnapshot;
import pe.entregago.domain.model.OrderConfirmationResult;
import pe.entregago.domain.model.OrderRejectedException;
import pe.entregago.domain.model.FailureReason;
import pe.entregago.domain.model.PaymentAuthorization;
import pe.entregago.domain.model.PricingLine;
import pe.entregago.domain.model.PriceBreakdown;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runnable in-memory adapter for the academic sample; replace with transactional persistence in production. */
@Component
public class InMemoryMarketplaceAdapter implements MerchantRepository, AddressRepository,
        ProductCatalog, CoveragePolicy, InventoryPort, PricingService, PaymentGateway, OrderRepository {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final Map<String, MerchantSnapshot> merchants = new ConcurrentHashMap<>();
    private final Map<String, AddressSnapshot> addresses = new ConcurrentHashMap<>();
    private final Map<String, ProductState> products = new ConcurrentHashMap<>();
    private final Map<String, CouponOffer> coupons = new ConcurrentHashMap<>();
    private final Map<String, Integer> couponUseCounts = new ConcurrentHashMap<>();
    private final Map<String, ReservationState> reservations = new ConcurrentHashMap<>();
    private final Map<String, Boolean> paymentOutcomes = new ConcurrentHashMap<>();
    private final Map<String, OrderConfirmationResult> savedOrders = new ConcurrentHashMap<>();
    private BigDecimal shipping = new BigDecimal("4.00");
    private BigDecimal taxRate = new BigDecimal("0.10");

    @Override
    public Optional<MerchantSnapshot> findMerchant(String merchantId) {
        return Optional.ofNullable(merchants.get(merchantId));
    }

    @Override
    public Optional<AddressSnapshot> findAddress(String addressId) {
        return Optional.ofNullable(addresses.get(addressId));
    }

    @Override
    public synchronized Optional<ProductSnapshot> findProduct(String productId) {
        ProductState product = products.get(productId);
        return product == null ? Optional.empty() : Optional.of(product.snapshot());
    }

    @Override
    public boolean covers(MerchantSnapshot merchant, AddressSnapshot address) {
        return merchant.zoneId().equals(address.zoneId());
    }

    @Override
    public synchronized ReservationToken reserveAll(List<ReservationLine> lines) {
        Map<String, Integer> requestedByProduct = new HashMap<>();
        for (ReservationLine line : lines) {
            requestedByProduct.merge(line.productId(), line.quantity(), Integer::sum);
        }
        requestedByProduct.forEach((productId, quantity) -> {
            ProductState product = products.get(productId);
            if (product == null || !product.available || product.availableStock() < quantity) {
                throw new OrderRejectedException(FailureReason.INSUFFICIENT_STOCK);
            }
        });
        requestedByProduct.forEach((productId, quantity) -> products.get(productId).reserved += quantity);
        ReservationToken token = new ReservationToken(UUID.randomUUID().toString());
        reservations.put(token.id(), new ReservationState(requestedByProduct, true));
        return token;
    }

    @Override
    public synchronized void release(ReservationToken token) {
        ReservationState reservation = reservations.get(token.id());
        if (reservation == null || !reservation.active) {
            return;
        }
        reservation.quantities.forEach((productId, quantity) -> {
            ProductState product = products.get(productId);
            if (product != null) {
                product.reserved -= quantity;
            }
        });
        reservation.active = false;
    }

    @Override
    public synchronized PriceBreakdown quote(String merchantId, List<PricingLine> lines, String couponCode) {
        BigDecimal subtotal = lines.stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = ZERO;
        String appliedCoupon = null;
        String rejectionReason = null;

        if (couponCode != null && !couponCode.isBlank()) {
            CouponOffer offer = coupons.get(couponCode);
            rejectionReason = couponRejectionReason(offer, merchantId, subtotal);
            if (rejectionReason == null) {
                BigDecimal calculated = subtotal.multiply(offer.percentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                discount = calculated.min(offer.maximumDiscount()).min(subtotal);
                appliedCoupon = offer.code();
            }
        }

        BigDecimal taxableAmount = subtotal.subtract(discount).max(ZERO);
        BigDecimal tax = taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxableAmount.add(shipping).add(tax).setScale(2, RoundingMode.HALF_UP);
        return new PriceBreakdown(subtotal, discount, shipping, tax, total, appliedCoupon, rejectionReason);
    }

    private String couponRejectionReason(CouponOffer offer, String merchantId, BigDecimal subtotal) {
        if (offer == null) return "COUPON_NOT_FOUND";
        LocalDate today = LocalDate.now();
        if (today.isBefore(offer.validFrom()) || today.isAfter(offer.validUntil())) return "COUPON_EXPIRED";
        if (offer.merchantId() != null && !offer.merchantId().equals(merchantId)) return "MERCHANT_NOT_APPLICABLE";
        if (subtotal.compareTo(offer.minimumSubtotal()) < 0) return "MINIMUM_NOT_MET";
        int uses = couponUseCounts.getOrDefault(offer.code(), offer.uses());
        if (offer.maximumUses() > 0 && uses >= offer.maximumUses()) return "USAGE_LIMIT_REACHED";
        return null;
    }

    @Override
    public PaymentAuthorization authorize(String paymentMethodId, BigDecimal amount, String orderId) {
        boolean approved = paymentOutcomes.getOrDefault(paymentMethodId, true);
        return new PaymentAuthorization("PAY-" + UUID.randomUUID(), approved);
    }

    @Override
    public void voidAuthorization(PaymentAuthorization authorization) {
        // No external payment provider is called in this in-memory adapter.
    }

    @Override
    public synchronized void saveConfirmed(OrderConfirmationResult order) {
        savedOrders.put(order.orderId(), order);
        String usedCoupon = order.pricing().appliedCouponCode();
        if (usedCoupon != null) {
            couponUseCounts.merge(usedCoupon, 1, Integer::sum);
        }
    }

    public void resetScenario() {
        merchants.clear();
        addresses.clear();
        products.clear();
        coupons.clear();
        couponUseCounts.clear();
        reservations.clear();
        paymentOutcomes.clear();
        savedOrders.clear();
        shipping = new BigDecimal("4.00");
        taxRate = new BigDecimal("0.10");
    }

    public void seedMerchant(String id, boolean open, String zoneId) {
        merchants.put(id, new MerchantSnapshot(id, open, zoneId));
    }

    public void seedAddress(String id, String customerId, String zoneId) {
        addresses.put(id, new AddressSnapshot(id, customerId, zoneId));
    }

    public void seedProduct(String id, String merchantId, String name, BigDecimal price, int stock) {
        products.put(id, new ProductState(id, merchantId, name, price, stock));
    }

    public void seedCoupon(CouponOffer offer) {
        coupons.put(offer.code(), offer);
        couponUseCounts.put(offer.code(), offer.uses());
    }

    public void setPaymentApproved(String paymentMethodId, boolean approved) {
        paymentOutcomes.put(paymentMethodId, approved);
    }

    public void setPricingPolicy(BigDecimal shipping, BigDecimal taxRate) {
        this.shipping = shipping.setScale(2, RoundingMode.HALF_UP);
        this.taxRate = taxRate;
    }

    public synchronized int reservedQuantity(String productId) {
        ProductState product = products.get(productId);
        return product == null ? 0 : product.reserved;
    }

    public synchronized int availableQuantity(String productId) {
        ProductState product = products.get(productId);
        return product == null ? 0 : product.availableStock();
    }

    public boolean hasConfirmedOrder(String orderId) {
        return savedOrders.containsKey(orderId);
    }

    private static final class ProductState {
        private final String id;
        private final String merchantId;
        private final String name;
        private final BigDecimal unitPrice;
        private final int onHand;
        private final boolean available = true;
        private int reserved;

        private ProductState(String id, String merchantId, String name, BigDecimal unitPrice, int onHand) {
            this.id = id;
            this.merchantId = merchantId;
            this.name = name;
            this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
            this.onHand = onHand;
        }

        private int availableStock() {
            return onHand - reserved;
        }

        private ProductSnapshot snapshot() {
            return new ProductSnapshot(id, merchantId, name, unitPrice, availableStock(), available);
        }
    }

    private static final class ReservationState {
        private final Map<String, Integer> quantities;
        private boolean active;

        private ReservationState(Map<String, Integer> quantities, boolean active) {
            this.quantities = Map.copyOf(quantities);
            this.active = active;
        }
    }
}
