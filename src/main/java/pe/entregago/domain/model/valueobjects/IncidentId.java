package pe.entregago.domain.model.valueobjects;

public record IncidentId(String value) {
    public IncidentId { CustomerId.requireText(value, "incidentId"); }
}
