package pe.entregago.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, String currency) implements Comparable<Money> {
    public Money {
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("amount must be non-negative");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency is required");
        currency = currency.toUpperCase();
        Currency.getInstance(currency);
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money soles(String amount) { return new Money(new BigDecimal(amount), "PEN"); }
    public static Money soles(BigDecimal amount) { return new Money(amount, "PEN"); }
    public static Money zeroPEN() { return soles("0.00"); }
    public Money add(Money other) { requireSameCurrency(other); return soles(amount.add(other.amount)); }
    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = amount.subtract(other.amount);
        if (result.signum() < 0) throw new IllegalArgumentException("money cannot become negative");
        return new Money(result, currency);
    }
    public Money multiply(BigDecimal factor) {
        if (factor.signum() < 0) throw new IllegalArgumentException("factor cannot be negative");
        return new Money(amount.multiply(factor), currency);
    }
    public Money min(Money other) { requireSameCurrency(other); return compareTo(other) <= 0 ? this : other; }
    private void requireSameCurrency(Money other) {
        if (other == null || !currency.equals(other.currency)) throw new IllegalArgumentException("currency mismatch");
    }
    @Override public int compareTo(Money other) { requireSameCurrency(other); return amount.compareTo(other.amount); }
}
