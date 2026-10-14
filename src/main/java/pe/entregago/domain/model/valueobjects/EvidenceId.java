package pe.entregago.domain.model.valueobjects;

public record EvidenceId(String value) {
    public EvidenceId { CustomerId.requireText(value, "evidenceId"); }
}
