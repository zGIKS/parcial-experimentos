package pe.entregago.interfaces.rest.resources;
import pe.entregago.application.ConfirmOrderResult;
public record OrderConfirmationResource(String orderId, String status, String reservationId,
        String paymentReference, String subtotal, String discount, String shipping, String tax, String total,
        String appliedCouponCode, String couponRejectionReason) {
    public static OrderConfirmationResource from(ConfirmOrderResult result) {
        var price=result.price();
        return new OrderConfirmationResource(result.orderId().value(), result.status().name(), result.reservationId().value(),
                result.paymentReference(), price.subtotal().amount().toPlainString(), price.discount().amount().toPlainString(),
                price.shipping().amount().toPlainString(), price.tax().amount().toPlainString(), price.total().amount().toPlainString(),
                price.appliedCouponCode(), price.couponRejectionReason());
    }
}
