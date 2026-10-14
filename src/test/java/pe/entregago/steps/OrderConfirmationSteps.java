package pe.entregago.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import pe.entregago.domain.model.CouponOffer;
import pe.entregago.infrastructure.memory.InMemoryMarketplaceAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderConfirmationSteps {
    @LocalServerPort
    int port;

    @Autowired
    InMemoryMarketplaceAdapter marketplace;

    private final Map<String, Integer> quantities = new LinkedHashMap<>();
    private String customerId;
    private String merchantId;
    private String addressId;
    private String couponCode;
    private String paymentMethodId;
    private Response response;

    @Before
    public void resetScenario() {
        marketplace.resetScenario();
        quantities.clear();
        couponCode = null;
        paymentMethodId = "PM-OK";
        response = null;
        RestAssured.port = port;
    }

    @Given("el comercio {string} está {string}")
    public void merchantState(String id, String state) {
        merchantId = id;
        marketplace.seedMerchant(id, state.equalsIgnoreCase("abierto"), "Z-01");
    }

    @Given("la dirección {string} del cliente {string} tiene cobertura {string} respecto del comercio")
    public void customerAddressCoverage(String id, String ownerId, String coverageState) {
        addressId = id;
        customerId = ownerId;
        String zoneId = coverageState.equalsIgnoreCase("cubierta") ? "Z-01" : "Z-OUTSIDE";
        marketplace.seedAddress(id, ownerId, zoneId);
    }

    @Given("la política de precios aplica envío de {double} PEN e impuesto de {double} por ciento")
    public void pricingPolicy(double shipping, double taxPercent) {
        marketplace.setPricingPolicy(money(shipping), BigDecimal.valueOf(taxPercent).movePointLeft(2));
    }

    @Given("el catálogo contiene estos productos:")
    public void catalogProducts(DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String productId = row.get("productId");
            marketplace.seedProduct(productId, merchantId, row.get("name"), money(row.get("unitPrice")),
                    Integer.parseInt(row.get("stock")));
            quantities.put(productId, Integer.parseInt(row.get("quantity")));
        }
    }

    @Given("el producto {string} tiene stock {int} unidades y se solicita 1 unidad")
    public void singleProduct(String productId, int stock) {
        marketplace.seedProduct(productId, merchantId, "Producto de prueba", new BigDecimal("10.00"), stock);
        quantities.put(productId, 1);
    }

    @Given("el cupón {string} ofrece {double} por ciento con máximo de {double} PEN")
    public void validCoupon(String code, double percentage, double maxDiscount) {
        couponCode = code;
        marketplace.seedCoupon(new CouponOffer(code, merchantId, LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(30), BigDecimal.ZERO, BigDecimal.valueOf(percentage),
                money(maxDiscount), 100, 0));
    }

    @Given("el cupón {string} está en condición {string}")
    public void couponCondition(String code, String condition) {
        couponCode = code;
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate until = LocalDate.now().plusDays(30);
        String allowedMerchant = merchantId;
        BigDecimal minimum = BigDecimal.ZERO;
        int maximumUses = 100;
        int uses = 0;
        switch (condition) {
            case "VENCIDO" -> until = LocalDate.now().minusDays(1);
            case "MINIMO_NO_ALCANZADO" -> minimum = new BigDecimal("50.00");
            case "COMERCIO_NO_APLICABLE" -> allowedMerchant = "M-OTRO";
            case "LIMITE_AGOTADO" -> {
                maximumUses = 1;
                uses = 1;
            }
            default -> throw new IllegalArgumentException("Condición de cupón desconocida: " + condition);
        }
        marketplace.seedCoupon(new CouponOffer(code, allowedMerchant, from, until, minimum,
                new BigDecimal("10"), new BigDecimal("3.00"), maximumUses, uses));
    }

    @Given("el medio de pago {string} responde {string}")
    public void paymentOutcome(String paymentMethodId, String result) {
        this.paymentMethodId = paymentMethodId;
        marketplace.setPaymentApproved(paymentMethodId, result.equalsIgnoreCase("APPROVED"));
    }

    @When("el cliente {string} confirma el pedido en el comercio {string} para la dirección {string}")
    public void confirmOrder(String ownerId, String merchant, String address) {
        customerId = ownerId;
        merchantId = merchant;
        addressId = address;
        List<Map<String, Object>> items = new ArrayList<>();
        quantities.forEach((productId, quantity) -> items.add(Map.of("productId", productId, "quantity", quantity)));
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("customerId", customerId);
        request.put("merchantId", merchantId);
        request.put("addressId", addressId);
        request.put("paymentMethodId", paymentMethodId);
        request.put("couponCode", couponCode);
        request.put("items", items);
        response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/api/orders/confirm");
    }

    @Then("la API responde con HTTP {int}")
    public void apiStatus(int expectedStatus) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
    }

    @Then("el pedido queda en estado {string}")
    public void orderStatus(String expectedStatus) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(expectedStatus);
    }

    @Then("el descuento calculado es {double} PEN")
    public void discount(double expected) {
        assertThat(new BigDecimal(response.jsonPath().getString("discount")))
                .isEqualByComparingTo(money(expected));
    }

    @Then("el total calculado es {double} PEN")
    public void total(double expected) {
        assertThat(new BigDecimal(response.jsonPath().getString("total")))
                .isEqualByComparingTo(money(expected));
    }

    @Then("la reserva de {string} es {int} unidades")
    public void reservation(String productId, int expectedQuantity) {
        assertThat(marketplace.reservedQuantity(productId)).isEqualTo(expectedQuantity);
    }

    @Then("la API rechaza con código {string}")
    public void rejectionCode(String expectedCode) {
        assertThat(response.jsonPath().getString("code")).isEqualTo(expectedCode);
    }

    @Then("el motivo de rechazo del cupón es {string}")
    public void couponRejection(String expectedReason) {
        assertThat(response.jsonPath().getString("couponRejectionReason")).isEqualTo(expectedReason);
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
