package pe.entregago.domain.services;

import pe.entregago.domain.model.aggregates.Order;
import pe.entregago.domain.model.enums.OrderStatus;
import pe.entregago.domain.model.entities.OrderLine;
import pe.entregago.domain.model.valueobjects.MerchantId;
import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.model.valueobjects.ProductId;
import pe.entregago.domain.model.valueobjects.SalesReport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class StandardMerchantAnalyticsService implements MerchantAnalyticsService {
    @Override public SalesReport summarize(MerchantId merchantId, LocalDate from, LocalDate to, List<Order> orders) {
        List<Order> periodOrders=orders.stream().filter(order -> order.merchantId().equals(merchantId))
                .filter(order -> {
                    LocalDate created=order.createdAt().atZone(ZoneOffset.UTC).toLocalDate();
                    return !created.isBefore(from) && !created.isAfter(to);
                }).toList();
        Money total=periodOrders.stream().filter(order -> order.status() != OrderStatus.CANCELLED && order.status() != OrderStatus.REJECTED)
                .map(order -> order.price().total()).reduce(Money.zeroPEN(), Money::add);
        int cancellations=(int) periodOrders.stream().filter(order -> order.status() == OrderStatus.CANCELLED || order.status() == OrderStatus.REJECTED).count();
        List<Long> preparationSeconds=periodOrders.stream().flatMap(order -> order.preparationDuration().stream())
                .map(java.time.Duration::getSeconds).toList();
        BigDecimal averageMinutes=preparationSeconds.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(preparationSeconds.stream().mapToLong(Long::longValue).average().orElse(0) / 60.0)
                    .setScale(2, RoundingMode.HALF_UP);
        Map<ProductId,Integer> popularProducts=periodOrders.stream().filter(order -> order.status() != OrderStatus.CANCELLED)
                .flatMap(order -> order.lines().stream()).collect(Collectors.toMap(OrderLine::productId,
                        line -> line.quantity().value(), Integer::sum));
        return new SalesReport(from, to, total, cancellations, averageMinutes, popularProducts);
    }
}
