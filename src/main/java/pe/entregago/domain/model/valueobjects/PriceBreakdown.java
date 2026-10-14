package pe.entregago.domain.model.valueobjects;

public record PriceBreakdown(
        Money subtotal,
        Money discount,
        Money shipping,
        Money tax,
        Money total,
        String appliedCouponCode,
        String couponRejectionReason) {
    public PriceBreakdown {
        Money expected = subtotal.subtract(discount).add(shipping).add(tax);
        if (expected.compareTo(total) != 0) throw new IllegalArgumentException("total does not match its components");
    }
}
