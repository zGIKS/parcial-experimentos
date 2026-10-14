package pe.entregago.domain.model.events;
import pe.entregago.domain.model.enums.DeliveryStatus;
import pe.entregago.domain.model.valueobjects.DeliveryId;
import pe.entregago.domain.model.valueobjects.GeoLocation;
import java.time.Instant;
public record DeliveryStatusChanged(DeliveryId deliveryId, DeliveryStatus status, Instant occurredAt, GeoLocation location) implements DomainEvent { }
