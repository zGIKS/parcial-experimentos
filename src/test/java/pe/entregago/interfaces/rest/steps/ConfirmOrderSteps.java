package pe.entregago.interfaces.rest.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import pe.entregago.domain.model.aggregates.Merchant;
import pe.entregago.domain.model.entities.Coupon;
import pe.entregago.domain.model.entities.CustomerAddress;
import pe.entregago.domain.model.entities.OpeningHours;
import pe.entregago.domain.model.entities.Product;
import pe.entregago.domain.model.enums.DiscountType;
import pe.entregago.domain.model.valueobjects.*;
import pe.entregago.infrastructure.payment.InMemoryPaymentGateway;
import pe.entregago.infrastructure.persistence.memory.InMemoryMarketplaceAdapter;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfirmOrderSteps {
    private static final CustomerId CUSTOMER=new CustomerId("C-1");
    private static final AddressId ADDRESS=new AddressId("A-1");
    @LocalServerPort int port;
    @Autowired InMemoryMarketplaceAdapter marketplace;
    @Autowired InMemoryPaymentGateway paymentGateway;
    private final List<Map<String,Object>> requestedItems=new ArrayList<>();
    private String merchantId="M-1";
    private String couponCode;
    private Response response;

    @Before public void resetScenario() {
        marketplace.clear();
        paymentGateway.setRejectPayments(false);
        requestedItems.clear();
        merchantId="M-1";
        couponCode=null;
        response=null;
        RestAssured.baseURI="http://localhost";
        RestAssured.port=port;
    }

    @Given("merchant {string} is {string}, merchant zone {string}, customer zone {string} and payment {string}")
    public void configureMarketplace(String id, String state, String merchantZone, String customerZone, String payment) {
        merchantId=id;
        CoverageZoneId merchantCoverage=new CoverageZoneId(merchantZone);
        boolean open=state.equalsIgnoreCase("open");
        List<OpeningHours> hours=open ? List.of(new OpeningHours(Set.of(DayOfWeek.values()), LocalTime.MIDNIGHT, LocalTime.of(23,59))) : List.of();
        marketplace.put(new Merchant(new MerchantId(id), "Comercio EntregaGo", Set.of(merchantCoverage), hours, 25, true));
        marketplace.put(new CustomerAddress(ADDRESS, CUSTOMER, "Casa", "Av. Lima 123",
                new CoverageZoneId(customerZone), new GeoLocation(-12.04, -77.03)));
        paymentGateway.setRejectPayments(payment.equalsIgnoreCase("DECLINED"));
    }

    @Given("the catalog contains:")
    public void addCatalog(DataTable table) {
        for (Map<String,String> row : table.asMaps(String.class, String.class)) {
            marketplace.put(new Product(new ProductId(row.get("productId")), new MerchantId(merchantId), row.get("name"),
                    row.get("category"), Money.soles(row.get("price")), Integer.parseInt(row.get("stock")), true));
        }
    }

    @Given("product {string} has price {string} and stock {int}")
    public void addProduct(String productId, String price, int stock) {
        marketplace.put(new Product(new ProductId(productId), new MerchantId(merchantId), "Producto " + productId,
                "General", Money.soles(price), stock, true));
    }

    @Given("customer requests these products:")
    public void setRequestedProducts(DataTable table) {
        requestedItems.clear();
        for (Map<String,String> row : table.asMaps(String.class, String.class)) {
            requestedItems.add(item(row.get("productId"), Integer.parseInt(row.get("quantity")), row.getOrDefault("observations", "")));
        }
    }

    @Given("customer requests product {string} quantity {int}")
    public void requestProduct(String productId, int quantity) {
        requestedItems.clear();
        requestedItems.add(item(productId, quantity, ""));
    }

    @Given("the coupon condition is {string}")
    public void configureCoupon(String condition) {
        LocalDate today=LocalDate.now(java.time.Clock.systemUTC());
        LocalDate from=today.minusDays(1), until=today.plusDays(1);
        Money minimum=Money.soles("10.00");
        MerchantId applicableMerchant=new MerchantId(merchantId);
        int maximumUses=10, uses=0;
        switch (condition) {
            case "EXPIRED" -> until=today.minusDays(1);
            case "BELOW_MINIMUM" -> minimum=Money.soles("100.00");
            case "OTHER_MERCHANT" -> applicableMerchant=new MerchantId("M-OTHER");
            case "LIMIT_REACHED" -> { maximumUses=1; uses=1; }
            default -> { }
        }
        Coupon coupon=new Coupon(new CouponCode("SAVE10"), from, until, minimum, applicableMerchant,
                maximumUses, uses, DiscountType.PERCENTAGE, new BigDecimal("10"), Money.soles("5.00"));
        marketplace.put(coupon);
        couponCode="SAVE10";
    }

    @When("the customer submits order confirmation")
    public void submitOrderConfirmation() {
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("customerId", CUSTOMER.value());
        body.put("merchantId", merchantId);
        body.put("addressId", ADDRESS.value());
        body.put("paymentMethodId", "CARD-1");
        body.put("couponCode", couponCode);
        body.put("items", List.copyOf(requestedItems));
        response=RestAssured.given().contentType("application/json").body(body).post("/api/orders/confirm");
    }

    @Then("the API responds with HTTP {int}")
    public void assertStatus(int expectedStatus) { assertThat(response.statusCode()).isEqualTo(expectedStatus); }

    @Then("the order is {string} with total {string}")
    public void assertConfirmedOrder(String expectedStatus, String expectedTotal) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(expectedStatus);
        assertThat(response.jsonPath().getString("total")).isEqualTo(expectedTotal);
    }

    @Then("stock for {string} has {int} reserved units")
    public void assertReservedStock(String productId, int expectedReserved) {
        assertThat(marketplace.product(new ProductId(productId)).orElseThrow().reserved()).isEqualTo(expectedReserved);
    }

    @Then("there are {int} saved orders")
    public void assertSavedOrders(int count) { assertThat(marketplace.allOrders()).hasSize(count); }

    @Then("coupon discount is {string} and rejection reason is {string}")
    public void assertCouponDecision(String discount, String rejection) {
        assertThat(response.jsonPath().getString("discount")).isEqualTo(discount);
        String actualRejection=response.jsonPath().getString("couponRejectionReason");
        if (rejection.equals("NONE")) assertThat(actualRejection).isNull();
        else assertThat(actualRejection).isEqualTo(rejection);
    }

    private static Map<String,Object> item(String productId, int quantity, String observations) {
        return Map.of("productId", productId, "quantity", quantity, "observations", observations == null ? "" : observations);
    }
}
