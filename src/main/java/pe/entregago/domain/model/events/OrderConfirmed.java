package pe.entregago.domain.model.events;
import pe.entregago.domain.model.valueobjects.OrderId;
import java.time.Instant;
public record OrderConfirmed(OrderId orderId, Instant occurredAt) implements DomainEvent { }
