package pe.entregago.domain.model.valueobjects;

public record MerchantId(String value) {
    public MerchantId { CustomerId.requireText(value, "merchantId"); }
}
