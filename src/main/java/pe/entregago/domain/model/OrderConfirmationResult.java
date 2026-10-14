package pe.entregago.domain.model;

import java.util.List;

public record OrderConfirmationResult(
        String orderId,
        String customerId,
        String merchantId,
        OrderStatus status,
        PriceBreakdown pricing,
        List<RequestedItem> items,
        String paymentReference) {
    public OrderConfirmationResult {
        items = List.copyOf(items);
    }
}
