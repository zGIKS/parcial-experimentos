package pe.entregago.domain.model.aggregates;

import pe.entregago.domain.model.BusinessRuleViolation;
import pe.entregago.domain.model.entities.DeliveryEvidence;
import pe.entregago.domain.model.entities.DeliveryStatusHistory;
import pe.entregago.domain.model.entities.Incident;
import pe.entregago.domain.model.enums.BusinessRuleCode;
import pe.entregago.domain.model.enums.DeliveryStatus;
import pe.entregago.domain.model.events.DeliveryStatusChanged;
import pe.entregago.domain.model.valueobjects.CourierId;
import pe.entregago.domain.model.valueobjects.DeliveryId;
import pe.entregago.domain.model.valueobjects.GeoLocation;
import pe.entregago.domain.model.valueobjects.OrderId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Delivery {
    private final DeliveryId id;
    private final OrderId orderId;
    private CourierId courierId;
    private DeliveryStatus status=DeliveryStatus.ASSIGNED;
    private final List<DeliveryStatusHistory> history=new ArrayList<>();
    private final List<Incident> incidents=new ArrayList<>();
    private DeliveryEvidence evidence;
    public Delivery(DeliveryId id, OrderId orderId, CourierId courierId, Instant assignedAt, GeoLocation location) {
        this.id=id; this.orderId=orderId; this.courierId=courierId;
        history.add(new DeliveryStatusHistory(status, assignedAt, location));
    }
    public void acceptAssignment(Instant at, GeoLocation location) { require(DeliveryStatus.ASSIGNED); record(DeliveryStatus.ASSIGNED, at, location); }
    public void rejectAssignment() { require(DeliveryStatus.ASSIGNED); status=DeliveryStatus.CANCELLED; }
    public void reassign(CourierId nextCourier, Instant at, GeoLocation location) {
        if (status != DeliveryStatus.ASSIGNED && status != DeliveryStatus.CANCELLED) throw invalid("delivery can only be reassigned before pickup");
        courierId=nextCourier; status=DeliveryStatus.ASSIGNED; record(status, at, location);
    }
    public void markPickedUp(Instant at, GeoLocation location) { require(DeliveryStatus.ASSIGNED); record(DeliveryStatus.PICKED_UP, at, location); }
    public void markInTransit(Instant at, GeoLocation location) { require(DeliveryStatus.PICKED_UP); record(DeliveryStatus.IN_TRANSIT, at, location); }
    public void complete(DeliveryEvidence evidence, Instant at) {
        require(DeliveryStatus.IN_TRANSIT);
        if (evidence == null || !evidence.isValid()) throw new BusinessRuleViolation(BusinessRuleCode.DELIVERY_EVIDENCE_REQUIRED, "valid delivery evidence is required");
        this.evidence=evidence; record(DeliveryStatus.DELIVERED, at, evidence.location());
    }
    public void reportIncident(Incident incident, Instant at, GeoLocation location) {
        if (status == DeliveryStatus.DELIVERED || status == DeliveryStatus.CANCELLED) throw invalid("terminal delivery cannot report an incident");
        incident.openReview(); incidents.add(incident); record(DeliveryStatus.UNDER_REVIEW, at, location);
    }
    private void record(DeliveryStatus next, Instant at, GeoLocation location) { status=next; history.add(new DeliveryStatusHistory(next, at, location)); }
    private void require(DeliveryStatus expected) { if (status != expected) throw invalid("expected " + expected + " but delivery is " + status); }
    private BusinessRuleViolation invalid(String message) { return new BusinessRuleViolation(BusinessRuleCode.INVALID_DELIVERY_TRANSITION, message); }
    public DeliveryId id() { return id; }
    public OrderId orderId() { return orderId; }
    public CourierId courierId() { return courierId; }
    public DeliveryStatus status() { return status; }
    public List<DeliveryStatusHistory> history() { return List.copyOf(history); }
    public List<Incident> incidents() { return List.copyOf(incidents); }
    public DeliveryEvidence evidence() { return evidence; }
}
