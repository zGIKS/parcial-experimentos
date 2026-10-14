package pe.entregago.domain.model.valueobjects;

public record ProductId(String value) {
    public ProductId { CustomerId.requireText(value, "productId"); }
}
