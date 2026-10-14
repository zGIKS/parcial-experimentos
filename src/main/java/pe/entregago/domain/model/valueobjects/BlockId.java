package pe.entregago.domain.model.valueobjects;

public record BlockId(String value) {
    public BlockId { CustomerId.requireText(value, "blockId"); }
}
