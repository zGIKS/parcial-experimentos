package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.valueobjects.CoverageZoneId;
import pe.entregago.domain.model.valueobjects.GeoLocation;

public record CoverageZone(CoverageZoneId id, String name, double minLatitude, double maxLatitude,
                          double minLongitude, double maxLongitude, boolean enabled) {
    public boolean contains(GeoLocation location) {
        return enabled && location.latitude() >= minLatitude && location.latitude() <= maxLatitude
                && location.longitude() >= minLongitude && location.longitude() <= maxLongitude;
    }
}
