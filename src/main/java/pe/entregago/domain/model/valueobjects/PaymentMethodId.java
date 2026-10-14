package pe.entregago.domain.model.valueobjects;

public record PaymentMethodId(String value) {
    public PaymentMethodId { CustomerId.requireText(value, "paymentMethodId"); }
}
