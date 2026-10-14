package pe.entregago.domain.model.valueobjects;

public record CourierId(String value) {
    public CourierId { CustomerId.requireText(value, "courierId"); }
}
