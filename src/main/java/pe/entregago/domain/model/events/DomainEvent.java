package pe.entregago.domain.model.events;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
