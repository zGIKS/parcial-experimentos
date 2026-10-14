package pe.entregago.domain.port;

import pe.entregago.domain.model.OrderConfirmationResult;

public interface OrderRepository {
    void saveConfirmed(OrderConfirmationResult order);
}
