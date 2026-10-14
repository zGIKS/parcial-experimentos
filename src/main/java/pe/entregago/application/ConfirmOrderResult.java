package pe.entregago.application;

import pe.entregago.domain.model.enums.OrderStatus;
import pe.entregago.domain.model.valueobjects.OrderId;
import pe.entregago.domain.model.valueobjects.PriceBreakdown;
import pe.entregago.domain.model.valueobjects.ReservationId;
import pe.entregago.domain.model.entities.OrderLine;
import java.util.List;

public record ConfirmOrderResult(OrderId orderId, OrderStatus status, PriceBreakdown price,
                                List<OrderLine> lines, ReservationId reservationId, String paymentReference) {
    public ConfirmOrderResult { lines=List.copyOf(lines); }
}
