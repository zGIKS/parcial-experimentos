package pe.entregago.domain.repositories;
import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.model.valueobjects.OrderId;
import pe.entregago.domain.model.valueobjects.PaymentAuthorization;
import pe.entregago.domain.model.valueobjects.PaymentMethodId;
public interface PaymentGateway {
    PaymentAuthorization authorize(PaymentMethodId methodId, Money amount, OrderId orderId);
    void voidAuthorization(PaymentAuthorization authorization);
    void refund(PaymentAuthorization authorization, Money amount);
}
