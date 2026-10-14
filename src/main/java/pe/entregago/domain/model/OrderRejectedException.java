package pe.entregago.domain.model;

public class OrderRejectedException extends RuntimeException {
    private final FailureReason reason;

    public OrderRejectedException(FailureReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public FailureReason reason() {
        return reason;
    }
}
