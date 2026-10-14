package pe.entregago.interfaces.rest.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.entregago.domain.model.aggregates.Merchant;
import pe.entregago.domain.model.entities.CustomerAddress;
import pe.entregago.domain.model.entities.OpeningHours;
import pe.entregago.domain.model.entities.Product;
import pe.entregago.domain.model.valueobjects.*;
import pe.entregago.infrastructure.payment.InMemoryPaymentGateway;
import pe.entregago.infrastructure.persistence.memory.InMemoryMarketplaceAdapter;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class OrderConfirmationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InMemoryMarketplaceAdapter marketplace;
    @Autowired InMemoryPaymentGateway paymentGateway;
    private static final MerchantId MERCHANT=new MerchantId("M-1");
    private static final CustomerId CUSTOMER=new CustomerId("C-1");
    private static final CoverageZoneId ZONE=new CoverageZoneId("Z-CENTRO");

    @BeforeEach void seedMarketplace() {
        marketplace.clear();
        paymentGateway.setRejectPayments(false);
        marketplace.put(new Merchant(MERCHANT, "Comercio Central", Set.of(ZONE),
                List.of(new OpeningHours(Set.of(DayOfWeek.values()), LocalTime.MIDNIGHT, LocalTime.of(23,59))), 25, true));
        marketplace.put(new CustomerAddress(new AddressId("A-1"), CUSTOMER, "Casa", "Av. Lima 123", ZONE,
                new GeoLocation(-12.04, -77.03)));
        marketplace.put(new Product(new ProductId("P-1"), MERCHANT, "Hamburguesa", "Comida", Money.soles("10.00"), 5, true));
        marketplace.put(new Product(new ProductId("P-2"), MERCHANT, "Jugo", "Bebida", Money.soles("5.00"), 3, true));
    }

    @Test void approvedPaymentConfirmsOrderAndReservesEveryRequestedProduct() throws Exception {
        String request="""
                {"customerId":"C-1","merchantId":"M-1","addressId":"A-1","paymentMethodId":"CARD-1",
                 "items":[{"productId":"P-1","quantity":2,"observations":"sin cebolla"},
                          {"productId":"P-2","quantity":1,"observations":""}]}
                """;

        var result=mvc.perform(post("/api/orders/confirm").contentType(MediaType.APPLICATION_JSON).content(request)).andReturn();
        JsonNode response=objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(response.path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(response.path("subtotal").asText()).isEqualTo("25.00");
        assertThat(response.path("total").asText()).isEqualTo("31.50");
        assertThat(response.path("paymentReference").asText()).isNotBlank();

        assertThat(marketplace.product(new ProductId("P-1")).orElseThrow().reserved()).isEqualTo(2);
        assertThat(marketplace.product(new ProductId("P-2")).orElseThrow().reserved()).isEqualTo(1);
        assertThat(marketplace.allOrders()).hasSize(1);
    }
}
