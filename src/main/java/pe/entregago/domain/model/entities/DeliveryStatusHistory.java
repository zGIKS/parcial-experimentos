package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.enums.DeliveryStatus;
import pe.entregago.domain.model.valueobjects.GeoLocation;
import java.time.Instant;

public record DeliveryStatusHistory(DeliveryStatus status, Instant occurredAt, GeoLocation location) { }
