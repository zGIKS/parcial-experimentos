package pe.entregago.domain.model.valueobjects;

public record ReservationId(String value) {
    public ReservationId { CustomerId.requireText(value, "reservationId"); }
}
