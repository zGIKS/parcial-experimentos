package pe.entregago.domain.model.commands;

import pe.entregago.domain.model.valueobjects.*;
import java.util.List;

public record ConfirmOrderCommand(CustomerId customerId, MerchantId merchantId, AddressId addressId,
                                 PaymentMethodId paymentMethodId, CouponCode couponCode,
                                 List<RequestedItem> items) {
    public ConfirmOrderCommand { items=List.copyOf(items); }
}
