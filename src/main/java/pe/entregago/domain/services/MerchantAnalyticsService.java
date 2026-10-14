package pe.entregago.domain.services;

import pe.entregago.domain.model.aggregates.Order;
import pe.entregago.domain.model.valueobjects.MerchantId;
import pe.entregago.domain.model.valueobjects.SalesReport;
import java.time.LocalDate;
import java.util.List;

public interface MerchantAnalyticsService {
    SalesReport summarize(MerchantId merchantId, LocalDate from, LocalDate to, List<Order> orders);
}
