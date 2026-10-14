package pe.entregago.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.entregago.domain.model.valueobjects.Money;
import pe.entregago.domain.services.OrderPricingPolicy;
import pe.entregago.domain.services.StandardOrderPricingPolicy;
import java.math.BigDecimal;
import java.time.Clock;

@Configuration
public class MarketplaceConfiguration {
    @Bean public Clock clock() { return Clock.systemUTC(); }
    @Bean public OrderPricingPolicy orderPricingPolicy() {
        return new StandardOrderPricingPolicy(Money.soles("4.00"), new BigDecimal("0.10"));
    }
}
