package pe.entregago.domain.model;

import java.util.List;

public record OrderConfirmationCommand(
        String customerId,
        String merchantId,
        String addressId,
        String paymentMethodId,
        String couponCode,
        List<RequestedItem> items) {
    public OrderConfirmationCommand {
        items = List.copyOf(items);
    }
}
