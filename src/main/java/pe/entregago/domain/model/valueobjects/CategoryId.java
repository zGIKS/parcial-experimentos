package pe.entregago.domain.model.valueobjects;

public record CategoryId(String value) {
    public CategoryId { CustomerId.requireText(value, "categoryId"); }
}
