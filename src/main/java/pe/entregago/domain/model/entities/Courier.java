package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.valueobjects.CourierId;
import pe.entregago.domain.model.valueobjects.CoverageZoneId;
import java.util.Set;

public final class Courier {
    private final CourierId id;
    private final String identityNumber;
    private final String vehicle;
    private final int loadCapacity;
    private final int maximumConcurrentDeliveries;
    private final Set<CoverageZoneId> operatingZones;
    private boolean available;
    private int activeDeliveries;
    public Courier(CourierId id, String identityNumber, String vehicle, int loadCapacity,
                   int maximumConcurrentDeliveries, Set<CoverageZoneId> operatingZones, boolean available) {
        this.id=id; this.identityNumber=identityNumber; this.vehicle=vehicle; this.loadCapacity=loadCapacity;
        this.maximumConcurrentDeliveries=maximumConcurrentDeliveries; this.operatingZones=Set.copyOf(operatingZones); this.available=available;
    }
    public boolean isEligible(CoverageZoneId zone, int requiredCapacity) {
        return available && loadCapacity >= requiredCapacity && operatingZones.contains(zone)
                && activeDeliveries < maximumConcurrentDeliveries;
    }
    public void reserveDelivery(CoverageZoneId zone, int requiredCapacity) {
        if (!isEligible(zone, requiredCapacity)) throw new IllegalStateException("courier is not eligible");
        activeDeliveries++;
    }
    public void completeDelivery() { if (activeDeliveries > 0) activeDeliveries--; }
    public void setAvailable(boolean available) { this.available=available; }
    public CourierId id() { return id; }
    public String identityNumber() { return identityNumber; }
    public String vehicle() { return vehicle; }
    public int loadCapacity() { return loadCapacity; }
    public int maximumConcurrentDeliveries() { return maximumConcurrentDeliveries; }
    public Set<CoverageZoneId> operatingZones() { return operatingZones; }
    public boolean available() { return available; }
    public int activeDeliveries() { return activeDeliveries; }
}
