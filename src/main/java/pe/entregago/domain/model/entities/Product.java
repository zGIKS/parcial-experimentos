package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.BusinessRuleViolation;
import pe.entregago.domain.model.enums.BusinessRuleCode;
import pe.entregago.domain.model.valueobjects.MerchantId;
import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.model.valueobjects.ProductId;

public final class Product {
    private final ProductId id;
    private final MerchantId merchantId;
    private final String name;
    private final String category;
    private Money unitPrice;
    private int onHand;
    private int reserved;
    private boolean available;
    public Product(ProductId id, MerchantId merchantId, String name, String category, Money unitPrice, int onHand, boolean available) {
        if (onHand < 0) throw new IllegalArgumentException("stock cannot be negative");
        this.id=id; this.merchantId=merchantId; this.name=name; this.category=category;
        this.unitPrice=unitPrice; this.onHand=onHand; this.available=available;
    }
    public int availableQuantity() { return onHand - reserved; }
    public boolean canReserve(int quantity) { return available && quantity > 0 && quantity <= availableQuantity(); }
    public void reserve(int quantity) {
        if (!available) throw new BusinessRuleViolation(BusinessRuleCode.PRODUCT_UNAVAILABLE, "Product is unavailable: " + id.value());
        if (!canReserve(quantity)) throw new BusinessRuleViolation(BusinessRuleCode.INSUFFICIENT_STOCK, "Insufficient stock: " + id.value());
        reserved += quantity;
    }
    public void release(int quantity) {
        if (quantity <= 0 || quantity > reserved) throw new IllegalStateException("cannot release more than reserved stock");
        reserved -= quantity;
    }
    public void consumeReservation(int quantity) { release(quantity); onHand -= quantity; }
    public void changeAvailability(boolean available) { this.available=available; }
    public void changePrice(Money price) { this.unitPrice=price; }
    public ProductId id() { return id; }
    public MerchantId merchantId() { return merchantId; }
    public String name() { return name; }
    public String category() { return category; }
    public Money unitPrice() { return unitPrice; }
    public int onHand() { return onHand; }
    public int reserved() { return reserved; }
    public boolean available() { return available; }
}
