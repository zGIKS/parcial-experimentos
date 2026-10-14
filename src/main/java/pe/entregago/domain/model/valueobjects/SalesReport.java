package pe.entregago.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SalesReport(LocalDate from, LocalDate to, Money salesTotal, int cancellations,
                         BigDecimal averagePreparationMinutes, Map<ProductId, Integer> mostRequestedProducts) {
    public SalesReport { mostRequestedProducts=Map.copyOf(mostRequestedProducts); }
}
