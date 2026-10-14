package pe.entregago.domain.model.valueobjects;

public record DeliveryId(String value) {
    public DeliveryId { CustomerId.requireText(value, "deliveryId"); }
}
