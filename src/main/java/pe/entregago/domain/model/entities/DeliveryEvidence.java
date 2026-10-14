package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.enums.EvidenceType;
import pe.entregago.domain.model.valueobjects.EvidenceId;
import pe.entregago.domain.model.valueobjects.GeoLocation;
import java.time.Instant;

public record DeliveryEvidence(EvidenceId id, EvidenceType type, String value, Instant capturedAt, GeoLocation location) {
    public boolean isValid() { return id != null && type != null && value != null && !value.isBlank() && capturedAt != null; }
}
