package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.enums.ReservationStatus;
import pe.entregago.domain.model.valueobjects.ProductId;
import pe.entregago.domain.model.valueobjects.ReservationId;
import java.time.Instant;
import java.util.Map;

public final class StockReservation {
    private final ReservationId id;
    private final Map<ProductId, Integer> quantities;
    private final Instant reservedAt;
    private ReservationStatus status = ReservationStatus.ACTIVE;
    public StockReservation(ReservationId id, Map<ProductId, Integer> quantities, Instant reservedAt) {
        this.id=id; this.quantities=Map.copyOf(quantities); this.reservedAt=reservedAt;
    }
    public void release() { requireActive(); status=ReservationStatus.RELEASED; }
    public void commit() { requireActive(); status=ReservationStatus.COMMITTED; }
    private void requireActive() { if (status != ReservationStatus.ACTIVE) throw new IllegalStateException("reservation is not active"); }
    public ReservationId id() { return id; }
    public Map<ProductId, Integer> quantities() { return quantities; }
    public Instant reservedAt() { return reservedAt; }
    public ReservationStatus status() { return status; }
}
