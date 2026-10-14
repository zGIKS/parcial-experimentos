package pe.entregago.domain.port;

import pe.entregago.domain.model.ReservationLine;
import pe.entregago.domain.model.ReservationToken;
import java.util.List;

public interface InventoryPort {
    ReservationToken reserveAll(List<ReservationLine> lines);
    void release(ReservationToken reservation);
}
