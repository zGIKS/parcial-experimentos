package pe.entregago.domain.model.valueobjects;

public record PaymentAuthorization(String reference, boolean approved) {
    public PaymentAuthorization { CustomerId.requireText(reference, "paymentReference"); }
}
