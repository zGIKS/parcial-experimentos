package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.enums.IncidentStatus;
import pe.entregago.domain.model.valueobjects.IncidentId;
import java.time.Instant;

public final class Incident {
    private final IncidentId id;
    private final String reason;
    private final Instant reportedAt;
    private IncidentStatus status=IncidentStatus.OPEN;
    public Incident(IncidentId id, String reason, Instant reportedAt) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("incident reason is required");
        this.id=id; this.reason=reason; this.reportedAt=reportedAt;
    }
    public void openReview() { if (status == IncidentStatus.RESOLVED) throw new IllegalStateException("incident already resolved"); status=IncidentStatus.IN_REVIEW; }
    public void resolve() { status=IncidentStatus.RESOLVED; }
    public IncidentId id() { return id; }
    public String reason() { return reason; }
    public Instant reportedAt() { return reportedAt; }
    public IncidentStatus status() { return status; }
}
