package pe.entregago.domain.model.valueobjects;

public record CoverageZoneId(String value) {
    public CoverageZoneId { CustomerId.requireText(value, "coverageZoneId"); }
}
