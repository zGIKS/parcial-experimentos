package pe.entregago.domain.model.valueobjects;

public record CustomerId(String value) {
    public CustomerId { requireText(value, "customerId"); }
    static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }
}
