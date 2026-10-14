package pe.entregago.infrastructure.payment;

import org.springframework.stereotype.Component;
import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.model.valueobjects.OrderId;
import pe.entregago.domain.model.valueobjects.PaymentAuthorization;
import pe.entregago.domain.model.valueobjects.PaymentMethodId;
import pe.entregago.domain.repositories.PaymentGateway;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryPaymentGateway implements PaymentGateway {
    private final AtomicBoolean rejectNext=new AtomicBoolean();
    private final AtomicInteger voidCount=new AtomicInteger();
    @Override public PaymentAuthorization authorize(PaymentMethodId methodId, Money amount, OrderId orderId) {
        boolean approved=!rejectNext.getAndSet(false);
        return new PaymentAuthorization("PAY-" + UUID.randomUUID(), approved);
    }
    @Override public void voidAuthorization(PaymentAuthorization authorization) { voidCount.incrementAndGet(); }
    public void rejectNextAuthorization() { rejectNext.set(true); }
    public void setRejectPayments(boolean reject) { rejectNext.set(reject); }
    public int voidCount() { return voidCount.get(); }
}
