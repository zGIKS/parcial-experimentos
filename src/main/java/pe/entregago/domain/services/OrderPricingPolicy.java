package pe.entregago.domain.services;

import pe.entregago.domain.model.entities.Coupon;
import pe.entregago.domain.model.entities.OrderLine;
import pe.entregago.domain.model.valueobjects.MerchantId;
import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.model.valueobjects.PriceBreakdown;
import java.time.LocalDate;
import java.util.List;

public interface OrderPricingPolicy {
    PriceBreakdown quote(MerchantId merchantId, List<OrderLine> lines, Coupon coupon, LocalDate businessDate);
    Money shippingFor(MerchantId merchantId);
}
