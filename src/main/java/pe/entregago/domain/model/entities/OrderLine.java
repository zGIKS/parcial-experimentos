package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.model.valueobjects.ProductId;
import pe.entregago.domain.model.valueobjects.Quantity;
import java.math.BigDecimal;

public record OrderLine(ProductId productId, String productName, Money unitPrice,
                       Quantity quantity, String observations) {
    public Money subtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity.value())); }
}
