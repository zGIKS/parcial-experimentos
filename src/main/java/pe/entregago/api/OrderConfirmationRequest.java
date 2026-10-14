package pe.entregago.api;

import pe.entregago.domain.model.OrderConfirmationCommand;
import pe.entregago.domain.model.RequestedItem;
import java.util.List;

public record OrderConfirmationRequest(
        String customerId,
        String merchantId,
        String addressId,
        String paymentMethodId,
        String couponCode,
        List<ItemRequest> items) {

    public OrderConfirmationCommand toCommand() {
        return new OrderConfirmationCommand(
                customerId,
                merchantId,
                addressId,
                paymentMethodId,
                couponCode,
                items.stream().map(item -> new RequestedItem(item.productId(), item.quantity())).toList());
    }

    public record ItemRequest(String productId, int quantity) { }
}
