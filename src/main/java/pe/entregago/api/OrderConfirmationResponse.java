package pe.entregago.api;

import pe.entregago.domain.model.OrderConfirmationResult;
import java.math.BigDecimal;

public record OrderConfirmationResponse(
        String orderId,
        String status,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shipping,
        BigDecimal tax,
        BigDecimal total,
        String appliedCouponCode,
        String couponRejectionReason) {
    public static OrderConfirmationResponse from(OrderConfirmationResult result) {
        var pricing = result.pricing();
        return new OrderConfirmationResponse(
                result.orderId(), result.status().name(), pricing.subtotal(), pricing.discount(),
                pricing.shipping(), pricing.tax(), pricing.total(), pricing.appliedCouponCode(),
                pricing.couponRejectionReason());
    }
}
