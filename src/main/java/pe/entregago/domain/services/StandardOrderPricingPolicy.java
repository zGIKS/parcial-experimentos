package pe.entregago.domain.services;

import pe.entregago.domain.model.entities.Coupon;
import pe.entregago.domain.model.entities.OrderLine;
import pe.entregago.domain.model.valueobjects.CouponDecision;
import pe.entregago.domain.model.valueobjects.MerchantId;
import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.model.valueobjects.PriceBreakdown;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class StandardOrderPricingPolicy implements OrderPricingPolicy {
    private final Money shipping;
    private final BigDecimal taxRate;
    public StandardOrderPricingPolicy(Money shipping, BigDecimal taxRate) { this.shipping=shipping; this.taxRate=taxRate; }
    @Override public PriceBreakdown quote(MerchantId merchantId, List<OrderLine> lines, Coupon coupon, LocalDate date) {
        Money subtotal=lines.stream().map(OrderLine::subtotal).reduce(Money.zeroPEN(), Money::add);
        CouponDecision decision=coupon == null ? new CouponDecision(Money.zeroPEN(), null) : coupon.evaluate(merchantId, subtotal, date);
        Money discount=decision.discount();
        Money taxable=subtotal.subtract(discount);
        Money tax=taxable.multiply(taxRate);
        return new PriceBreakdown(subtotal, discount, shipping, tax, taxable.add(shipping).add(tax),
                decision.applied() && coupon != null ? coupon.code().value() : null, decision.rejectionCode());
    }
    @Override public Money shippingFor(MerchantId merchantId) { return shipping; }
}
