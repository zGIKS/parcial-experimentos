package pe.entregago.domain.port;

import pe.entregago.domain.model.PaymentAuthorization;
import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentAuthorization authorize(String paymentMethodId, BigDecimal amount, String orderId);
    void voidAuthorization(PaymentAuthorization authorization);
}
