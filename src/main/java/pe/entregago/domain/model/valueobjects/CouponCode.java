package pe.entregago.domain.model.valueobjects;

public record CouponCode(String value) {
    public CouponCode {
        CustomerId.requireText(value, "couponCode");
        value = value.trim().toUpperCase();
    }
}
