package pe.entregago.domain.repositories;
import pe.entregago.domain.model.entities.StockReservation;
import pe.entregago.domain.model.valueobjects.RequestedItem;
import pe.entregago.domain.model.valueobjects.ReservationId;
import java.time.Instant;
import java.util.List;
public interface StockReservationRepository {
    StockReservation reserveAll(ReservationId reservationId, List<RequestedItem> items, Instant reservedAt);
    void release(ReservationId reservationId);
    void commit(ReservationId reservationId);
}
