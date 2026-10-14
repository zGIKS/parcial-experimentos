package pe.entregago.domain.model.aggregates;

import pe.entregago.domain.model.valueobjects.CustomerId;
import pe.entregago.domain.model.valueobjects.MerchantId;
import pe.entregago.domain.model.valueobjects.ProductId;
import pe.entregago.domain.model.valueobjects.Quantity;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Cart {
    private final CustomerId customerId;
    private MerchantId merchantId;
    private final Map<ProductId, CartLine> lines = new LinkedHashMap<>();
    public Cart(CustomerId customerId) { this.customerId=customerId; }
    public void add(MerchantId merchantId, ProductId productId, Quantity quantity, String observations) {
        if (this.merchantId != null && !this.merchantId.equals(merchantId)) throw new IllegalStateException("a cart can contain one merchant at a time");
        this.merchantId=merchantId;
        lines.merge(productId, new CartLine(quantity, observations), CartLine::merge);
    }
    public void remove(ProductId productId) { lines.remove(productId); }
    public CustomerId customerId() { return customerId; }
    public MerchantId merchantId() { return merchantId; }
    public Map<ProductId, CartLine> lines() { return Map.copyOf(lines); }
    public record CartLine(Quantity quantity, String observations) {
        CartLine merge(CartLine other) { return new CartLine(quantity.plus(other.quantity), other.observations); }
    }
}
