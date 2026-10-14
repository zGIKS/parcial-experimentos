package pe.entregago.domain.model.events;
import pe.entregago.domain.model.valueobjects.OrderId;
import java.time.Instant;
public record OrderCancelled(OrderId orderId, String reason, Instant occurredAt) { }
