package pe.entregago.interfaces.rest.resources;
import pe.entregago.domain.model.commands.ConfirmOrderCommand;
import pe.entregago.domain.model.valueobjects.*;
import java.util.List;
public record ConfirmOrderRequest(String customerId, String merchantId, String addressId,
        String paymentMethodId, String couponCode, List<ItemResource> items) {
    public ConfirmOrderCommand toCommand() {
        return new ConfirmOrderCommand(new CustomerId(customerId), new MerchantId(merchantId), new AddressId(addressId),
                new PaymentMethodId(paymentMethodId), couponCode == null || couponCode.isBlank() ? null : new CouponCode(couponCode),
                items.stream().map(item -> new RequestedItem(new ProductId(item.productId()), new Quantity(item.quantity()), item.observations())).toList());
    }
    public record ItemResource(String productId, int quantity, String observations) { }
}
