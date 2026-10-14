package pe.entregago.domain.model.aggregates;

import pe.entregago.domain.model.enums.OrderStatus;
import pe.entregago.domain.model.enums.RefundType;
import pe.entregago.domain.model.valueobjects.Money;
import java.math.BigDecimal;

public record CancellationPolicy(BigDecimal refundAfterPreparationRate) {
    public CancellationPolicy {
        if (refundAfterPreparationRate == null || refundAfterPreparationRate.signum() < 0 || refundAfterPreparationRate.compareTo(BigDecimal.ONE) > 0)
            throw new IllegalArgumentException("refund rate must be between zero and one");
    }
    public RefundType refundFor(OrderStatus status, Money total) {
        if (status == OrderStatus.CONFIRMED || status == OrderStatus.MERCHANT_ACCEPTED) return RefundType.FULL;
        if (status == OrderStatus.IN_PREPARATION || status == OrderStatus.READY_FOR_PICKUP)
            return refundAfterPreparationRate.signum() == 0 ? RefundType.NONE
                    : refundAfterPreparationRate.compareTo(BigDecimal.ONE) == 0 ? RefundType.FULL : RefundType.PARTIAL;
        return RefundType.NONE;
    }
}
