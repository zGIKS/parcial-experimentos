package pe.entregago.domain.model.aggregates;

import pe.entregago.domain.model.entities.OpeningHours;
import pe.entregago.domain.model.valueobjects.CoverageZoneId;
import pe.entregago.domain.model.valueobjects.MerchantId;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

public final class Merchant {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");
    private final MerchantId id;
    private final String name;
    private final Set<CoverageZoneId> coverageZones;
    private final List<OpeningHours> openingHours;
    private final int preparationMinutes;
    private final boolean active;
    public Merchant(MerchantId id, String name, Set<CoverageZoneId> coverageZones,
                    List<OpeningHours> openingHours, int preparationMinutes, boolean active) {
        if (preparationMinutes < 0) throw new IllegalArgumentException("preparation time cannot be negative");
        this.id=id; this.name=name; this.coverageZones=Set.copyOf(coverageZones);
        this.openingHours=List.copyOf(openingHours); this.preparationMinutes=preparationMinutes; this.active=active;
    }
    public boolean isOpenAt(Instant instant) {
        if (!active) return false;
        ZonedDateTime localTime = instant.atZone(BUSINESS_ZONE);
        return openingHours.stream().anyMatch(hours -> hours.contains(localTime));
    }
    public boolean serves(CoverageZoneId zoneId) { return active && coverageZones.contains(zoneId); }
    public MerchantId id() { return id; }
    public String name() { return name; }
    public int preparationMinutes() { return preparationMinutes; }
    public Set<CoverageZoneId> coverageZones() { return coverageZones; }
}
