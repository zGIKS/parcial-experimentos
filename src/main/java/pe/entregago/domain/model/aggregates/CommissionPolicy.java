package pe.entregago.domain.model.aggregates;

import pe.entregago.domain.model.valueobjects.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CommissionPolicy(BigDecimal rate, LocalDate effectiveFrom, LocalDate effectiveUntil) {
    public CommissionPolicy {
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("commission rate must be between zero and one");
    }
    public boolean isEffectiveOn(LocalDate date) { return !date.isBefore(effectiveFrom) && !date.isAfter(effectiveUntil); }
    public Money commissionFor(Money salesAmount) { return salesAmount.multiply(rate); }
}
