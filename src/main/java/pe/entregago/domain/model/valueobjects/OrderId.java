package pe.entregago.domain.model.valueobjects;

import java.util.UUID;

public record OrderId(String value) {
    public OrderId { CustomerId.requireText(value, "orderId"); }
    public static OrderId generate() { return new OrderId(UUID.randomUUID().toString()); }
}
