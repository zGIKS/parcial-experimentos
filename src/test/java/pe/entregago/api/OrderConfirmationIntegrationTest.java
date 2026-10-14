package pe.entregago.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.entregago.infrastructure.memory.InMemoryMarketplaceAdapter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderConfirmationIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired InMemoryMarketplaceAdapter marketplace;

    @BeforeEach
    void setUpMarketplace() {
        marketplace.resetScenario();
        marketplace.seedMerchant("M-01", true, "Z-01");
        marketplace.seedAddress("A-01", "C-01", "Z-01");
        marketplace.seedProduct("P-01", "M-01", "Café", new BigDecimal("10.00"), 8);
        marketplace.setPaymentApproved("PM-OK", true);
    }

    @Test
    void shouldConfirmOrderAndReserveStockAcrossApiApplicationInventoryAndPayment() throws Exception {
        mockMvc.perform(post("/api/orders/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId":"C-01",
                                  "merchantId":"M-01",
                                  "addressId":"A-01",
                                  "paymentMethodId":"PM-OK",
                                  "items":[{"productId":"P-01","quantity":2}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.subtotal").value(20.00))
                .andExpect(jsonPath("$.total").value(26.00));

        assertThat(marketplace.reservedQuantity("P-01")).isEqualTo(2);
        assertThat(marketplace.availableQuantity("P-01")).isEqualTo(6);
    }
}
