package pe.entregago.domain.model.valueobjects;

public record RequestedItem(ProductId productId, Quantity quantity, String observations) { }
