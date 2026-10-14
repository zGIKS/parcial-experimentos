package pe.entregago.interfaces.rest.steps;

import pe.entregago.domain.model.entities.Coupon;
import pe.entregago.domain.model.entities.OrderLine;
import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.model.valueobjects.MerchantId;
import pe.entregago.domain.model.valueobjects.PriceBreakdown;
import pe.entregago.domain.services.OrderPricingPolicy;
import pe.entregago.domain.services.StandardOrderPricingPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Scenario-scoped pricing configuration used by the REST acceptance tests. */
final class ScenarioPricingPolicy implements OrderPricingPolicy {
    private volatile StandardOrderPricingPolicy delegate = policy("4.00", "0.10");

    void configure(String shipping, BigDecimal taxRate) {
        delegate = policy(shipping, taxRate.toPlainString());
    }

    void reset() {
        delegate = policy("4.00", "0.10");
    }

    @Override
    public PriceBreakdown quote(MerchantId merchantId, List<OrderLine> lines, Coupon coupon, LocalDate businessDate) {
        return delegate.quote(merchantId, lines, coupon, businessDate);
    }

    @Override
    public Money shippingFor(MerchantId merchantId) {
        return delegate.shippingFor(merchantId);
    }

    private static StandardOrderPricingPolicy policy(String shipping, String taxRate) {
        return new StandardOrderPricingPolicy(Money.soles(shipping), new BigDecimal(taxRate));
    }
}
