package pe.entregago.infrastructure.persistence.memory;

import org.springframework.stereotype.Repository;
import pe.entregago.domain.model.BusinessRuleViolation;
import pe.entregago.domain.model.aggregates.*;
import pe.entregago.domain.model.entities.*;
import pe.entregago.domain.model.enums.BusinessRuleCode;
import pe.entregago.domain.model.enums.ReservationStatus;
import pe.entregago.domain.model.valueobjects.*;
import pe.entregago.domain.repositories.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMarketplaceAdapter implements MerchantRepository, CustomerAddressRepository,
        ProductRepository, CouponRepository, StockReservationRepository, OrderRepository,
        CourierRepository, DeliveryRepository {
    private final Map<MerchantId, Merchant> merchants=new ConcurrentHashMap<>();
    private final Map<AddressId, CustomerAddress> addresses=new ConcurrentHashMap<>();
    private final Map<ProductId, Product> products=new ConcurrentHashMap<>();
    private final Map<CouponCode, Coupon> coupons=new ConcurrentHashMap<>();
    private final Map<ReservationId, StockReservation> reservations=new ConcurrentHashMap<>();
    private final Map<OrderId, Order> orders=new ConcurrentHashMap<>();
    private final Map<CourierId, Courier> couriers=new ConcurrentHashMap<>();
    private final Map<DeliveryId, Delivery> deliveries=new ConcurrentHashMap<>();
    @Override public Optional<Merchant> findById(MerchantId id) { return Optional.ofNullable(merchants.get(id)); }
    @Override public Optional<CustomerAddress> findById(AddressId id) { return Optional.ofNullable(addresses.get(id)); }
    @Override public Optional<Product> findById(ProductId id) { return Optional.ofNullable(products.get(id)); }
    @Override public Optional<Coupon> findByCode(CouponCode code) { return Optional.ofNullable(coupons.get(code)); }
    @Override public synchronized void recordUse(CouponCode code) {
        Coupon coupon=coupons.get(code);
        if (coupon == null) throw new IllegalStateException("coupon disappeared before redemption");
        coupon.recordUse();
    }
    @Override public synchronized StockReservation reserveAll(ReservationId id, List<RequestedItem> items, Instant at) {
        Map<ProductId,Integer> quantityByProduct=new LinkedHashMap<>();
        items.forEach(item -> quantityByProduct.merge(item.productId(), item.quantity().value(), Integer::sum));
        for (var entry : quantityByProduct.entrySet()) {
            Product product=products.get(entry.getKey());
            if (product == null) throw new BusinessRuleViolation(BusinessRuleCode.PRODUCT_NOT_FOUND, "product was not found: " + entry.getKey().value());
            if (!product.canReserve(entry.getValue())) throw new BusinessRuleViolation(BusinessRuleCode.INSUFFICIENT_STOCK, "insufficient stock: " + entry.getKey().value());
        }
        quantityByProduct.forEach((productId, quantity) -> products.get(productId).reserve(quantity));
        StockReservation reservation=new StockReservation(id, quantityByProduct, at);
        reservations.put(id, reservation);
        return reservation;
    }
    @Override public synchronized void release(ReservationId id) {
        StockReservation reservation=requiredReservation(id);
        if (reservation.status() == ReservationStatus.ACTIVE) {
            reservation.quantities().forEach((productId, quantity) -> products.get(productId).release(quantity));
            reservation.release();
        }
    }
    @Override public synchronized void commit(ReservationId id) {
        StockReservation reservation=requiredReservation(id);
        reservation.quantities().forEach((productId, quantity) -> products.get(productId).consumeReservation(quantity));
        reservation.commit();
    }
    @Override public Order save(Order order) { orders.put(order.id(), order); return order; }
    @Override public Optional<Order> findById(OrderId id) { return Optional.ofNullable(orders.get(id)); }
    @Override public synchronized List<Courier> findEligible(Order order, int requiredCapacity) {
        CustomerAddress address=addresses.get(order.deliveryAddressId());
        if (address == null) return List.of();
        return couriers.values().stream().filter(courier -> courier.isEligible(address.zoneId(), requiredCapacity)).toList();
    }
    @Override public Delivery save(Delivery delivery) { deliveries.put(delivery.id(), delivery); return delivery; }
    @Override public Optional<Delivery> findById(DeliveryId id) { return Optional.ofNullable(deliveries.get(id)); }
    public void put(Merchant merchant) { merchants.put(merchant.id(), merchant); }
    public void put(CustomerAddress address) { addresses.put(address.id(), address); }
    public void put(Product product) { products.put(product.id(), product); }
    public void put(Coupon coupon) { coupons.put(coupon.code(), coupon); }
    public void put(Courier courier) { couriers.put(courier.id(), courier); }
    public Optional<Coupon> coupon(CouponCode code) { return findByCode(code); }
    public Optional<Product> product(ProductId id) { return findById(id); }
    public Optional<StockReservation> reservation(ReservationId id) { return Optional.ofNullable(reservations.get(id)); }
    public List<Order> allOrders() { return List.copyOf(orders.values()); }
    public synchronized void clear() {
        merchants.clear(); addresses.clear(); products.clear(); coupons.clear(); reservations.clear();
        orders.clear(); couriers.clear(); deliveries.clear();
    }
    private StockReservation requiredReservation(ReservationId id) {
        StockReservation reservation=reservations.get(id);
        if (reservation == null) throw new IllegalArgumentException("reservation not found: " + id.value());
        return reservation;
    }
}
