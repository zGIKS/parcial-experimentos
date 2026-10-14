package pe.entregago.domain.model.aggregates;

import pe.entregago.domain.model.valueobjects.CustomerId;
import java.time.Instant;

public record PlatformAdmin(CustomerId id, String fullName) {
    public PlatformAdmin { if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("admin name is required"); }
    public TemporaryBlock block(String targetId, Instant until, String reason) { return new TemporaryBlock(targetId, until, reason, id); }
    public record TemporaryBlock(String targetId, Instant until, String reason, CustomerId createdBy) { }
}
