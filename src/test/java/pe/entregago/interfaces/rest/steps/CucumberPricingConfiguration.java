package pe.entregago.interfaces.rest.steps;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class CucumberPricingConfiguration {
    @Bean
    @Primary
    ScenarioPricingPolicy scenarioPricingPolicy() {
        return new ScenarioPricingPolicy();
    }
}
