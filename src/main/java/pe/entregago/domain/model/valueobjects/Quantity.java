package pe.entregago.domain.model.valueobjects;

public record Quantity(int value) {
    public Quantity {
        if (value <= 0) throw new IllegalArgumentException("quantity must be greater than zero");
    }
    public Quantity plus(Quantity other) { return new Quantity(value + other.value); }
}
