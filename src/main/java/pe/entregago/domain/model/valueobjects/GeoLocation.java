package pe.entregago.domain.model.valueobjects;

public record GeoLocation(double latitude, double longitude) {
    public GeoLocation {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("coordinates are outside the geographic range");
    }
}
