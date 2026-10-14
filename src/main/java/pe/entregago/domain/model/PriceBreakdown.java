package pe.entregago.domain.model;

import java.math.BigDecimal;

public record PriceBreakdown(
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shipping,
        BigDecimal tax,
        BigDecimal total,
        String appliedCouponCode,
        String couponRejectionReason) { }
