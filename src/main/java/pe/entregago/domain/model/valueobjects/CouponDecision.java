package pe.entregago.domain.model.valueobjects;

public record CouponDecision(Money discount, String rejectionCode) {
    public boolean applied() { return rejectionCode == null; }
}
