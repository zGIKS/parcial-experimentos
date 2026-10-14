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
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Cucumber system steps for the English and Spanish order-confirmation features. */
public class ConfirmOrderSteps {
    private static final CustomerId DEFAULT_CUSTOMER = new CustomerId("C-1");
    private static final AddressId DEFAULT_ADDRESS = new AddressId("A-1");
    private static final CoverageZoneId DEFAULT_ZONE = new CoverageZoneId("Z-CENTRO");

    @LocalServerPort
    int port;

    @Autowired
    InMemoryMarketplaceAdapter marketplace;
    @Autowired
    InMemoryPaymentGateway paymentGateway;
    @Autowired
    ScenarioPricingPolicy pricing;

    private final List<Map<String, Object>> requestedItems = new ArrayList<>();
    private String merchantId = "M-1";
    private CustomerId customerId = DEFAULT_CUSTOMER;
    private AddressId addressId = DEFAULT_ADDRESS;
    private String paymentMethodId = "CARD-1";
    private String couponCode;
    private BigDecimal couponPercent = new BigDecimal("10");
    private Money couponMaximumDiscount = Money.soles("5.00");
    private Response response;

    @Before
    public void resetScenario() {
        marketplace.clear();
        paymentGateway.setRejectPayments(false);
        pricing.reset();
        requestedItems.clear();
        merchantId = "M-1";
        customerId = DEFAULT_CUSTOMER;
        addressId = DEFAULT_ADDRESS;
        paymentMethodId = "CARD-1";
        couponCode = null;
        couponPercent = new BigDecimal("10");
        couponMaximumDiscount = Money.soles("5.00");
        response = null;
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    // English steps

    @Given("merchant {string} is {string}, merchant zone {string}, customer zone {string} and payment {string}")
    public void configureMarketplace(String id, String state, String merchantZone, String customerZone, String payment) {
        merchantId = id;
        customerId = DEFAULT_CUSTOMER;
        addressId = DEFAULT_ADDRESS;
        paymentMethodId = "CARD-1";
        CoverageZoneId merchantCoverage = new CoverageZoneId(merchantZone);
        storeMerchant(id, isOpen(state), merchantCoverage);
        marketplace.put(new CustomerAddress(addressId, customerId, "Casa", "Av. Lima 123",
                new CoverageZoneId(customerZone), new GeoLocation(-12.04, -77.03)));
        paymentGateway.setRejectPayments(!isApproved(payment));
    }

    @Given("the catalog contains:")
    public void addCatalog(DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
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
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
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
        putCoupon("SAVE10", condition);
    }

    @When("the customer submits order confirmation")
    public void submitOrderConfirmation() {
        sendConfirmation(paymentMethodId);
    }

    @Then("the API responds with HTTP {int}")
    public void assertStatus(int expectedStatus) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
    }

    @Then("the order is {string} with total {string}")
    public void assertConfirmedOrder(String expectedStatus, String expectedTotal) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(expectedStatus);
        assertThat(response.jsonPath().getString("total")).isEqualTo(expectedTotal);
    }

    @Then("stock for {string} has {int} reserved unit(s)")
    public void assertReservedStock(String productId, int expectedReserved) {
        assertThat(marketplace.product(new ProductId(productId)).orElseThrow().reserved()).isEqualTo(expectedReserved);
    }

    @Then("there are {int} saved orders")
    public void assertSavedOrders(int count) {
        assertThat(marketplace.allOrders()).hasSize(count);
    }

    @Then("coupon discount is {string} and rejection reason is {string}")
    public void assertCouponDecision(String discount, String rejection) {
        assertThat(response.jsonPath().getString("discount")).isEqualTo(discount);
        String actualRejection = response.jsonPath().getString("couponRejectionReason");
        if (rejection.equals("NONE")) assertThat(actualRejection).isNull();
        else assertThat(actualRejection).isEqualTo(rejection);
    }

    // Spanish steps used by confirmar-pedido.feature

    @Given("el comercio {string} está {string}")
    public void configurarComercio(String id, String estado) {
        merchantId = id;
        storeMerchant(id, isOpen(estado), DEFAULT_ZONE);
    }

    @Given("la dirección {string} del cliente {string} tiene cobertura {string} respecto del comercio")
    public void configurarDireccion(String idDireccion, String idCliente, String cobertura) {
        addressId = new AddressId(idDireccion);
        customerId = new CustomerId(idCliente);
        CoverageZoneId zone = isCovered(cobertura) ? DEFAULT_ZONE : new CoverageZoneId("Z-FUERA");
        marketplace.put(new CustomerAddress(addressId, customerId, "Casa", "Av. Lima 123", zone,
                new GeoLocation(-12.04, -77.03)));
    }

    @Given("la política de precios aplica envío de {string} PEN e impuesto de {int} por ciento")
    public void configurarPrecios(String costoEnvio, Integer porcentajeImpuesto) {
        pricing.configure(costoEnvio,
                BigDecimal.valueOf(porcentajeImpuesto).movePointLeft(2));
    }

    @Given("el catálogo contiene estos productos:")
    public void agregarCatalogo(DataTable table) {
        requestedItems.clear();
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String productId = tableValue(row, "productid", "idproducto", "codigoproducto", "productoid", "sku", "id", "producto");
            String name = optionalTableValue(row, "nombreproducto", "nombre", "productname").orElse("Producto " + productId);
            String category = optionalTableValue(row, "categoria", "category").orElse("General");
            String price = tableValue(row, "precio", "price", "preciopen", "preciounitario", "unitprice");
            int stock = Integer.parseInt(tableValue(row, "stock", "existencias", "stockinicial"));
            marketplace.put(new Product(new ProductId(productId), new MerchantId(merchantId), name,
                    category, Money.soles(price), stock, true));

            String quantity = optionalTableValue(row, "cantidadsolicitada", "cantidad", "quantity", "solicitado").orElse("1");
            String observations = optionalTableValue(row, "observaciones", "observations").orElse("");
            requestedItems.add(item(productId, Integer.parseInt(quantity), observations));
        }
    }

    @Given("el producto {string} tiene stock {int} unidades y se solicita {int} unidad")
    public void agregarProductoSolicitado(String productId, Integer stock, Integer quantity) {
        marketplace.put(new Product(new ProductId(productId), new MerchantId(merchantId), "Producto " + productId,
                "General", Money.soles("10.00"), stock, true));
        requestedItems.clear();
        requestedItems.add(item(productId, quantity, ""));
    }

    @Given("el cupón {string} ofrece {int} por ciento con máximo de {string} PEN")
    public void configurarOfertaCupon(String code, Integer percent, String maximumDiscount) {
        couponPercent = BigDecimal.valueOf(percent);
        couponMaximumDiscount = Money.soles(new BigDecimal(maximumDiscount));
        putCoupon(code, "VIGENTE");
    }

    @Given("el cupón {string} está en condición {string}")
    public void configurarCondicionCupon(String code, String condition) {
        couponCode = code;
        putCoupon(code, condition);
    }

    @Given("el medio de pago {string} responde {string}")
    public void configurarMedioDePago(String method, String state) {
        paymentMethodId = method;
        paymentGateway.setRejectPayments(!isApproved(state));
    }

    @When("el cliente {string} confirma el pedido en el comercio {string} para la dirección {string}")
    public void confirmarPedido(String idCliente, String idComercio, String idDireccion) {
        customerId = new CustomerId(idCliente);
        merchantId = idComercio;
        addressId = new AddressId(idDireccion);
        sendConfirmation(paymentMethodId);
    }

    @Then("la API responde con HTTP {int}")
    public void verificarHttp(int expectedStatus) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
    }

    @Then("la API rechaza con código {string}")
    public void verificarCodigoRechazo(String expectedCode) {
        assertThat(response.jsonPath().getString("code")).isEqualTo(expectedCode);
    }

    @Then("el pedido queda en estado {string}")
    public void verificarEstadoPedido(String expectedStatus) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(expectedStatus);
    }

    @Then("el descuento calculado es {string} PEN")
    public void verificarDescuento(String expectedDiscount) {
        assertThat(new BigDecimal(response.jsonPath().getString("discount")))
                .isEqualByComparingTo(new BigDecimal(expectedDiscount));
    }

    @Then("el total calculado es {string} PEN")
    public void verificarTotal(String expectedTotal) {
        assertThat(new BigDecimal(response.jsonPath().getString("total")))
                .isEqualByComparingTo(new BigDecimal(expectedTotal));
    }

    @Then("la reserva de {string} es {int} unidades")
    public void verificarReserva(String productId, Integer expectedReserved) {
        assertThat(marketplace.product(new ProductId(productId)).orElseThrow().reserved()).isEqualTo(expectedReserved);
    }

    @Then("el motivo de rechazo del cupón es {string}")
    public void verificarRechazoCupon(String expectedReason) {
        String actual = response.jsonPath().getString("couponRejectionReason");
        if (expectedReason.equals("NINGUNO") || expectedReason.equals("NONE")) assertThat(actual).isNull();
        else assertThat(actual).isEqualTo(expectedReason);
    }

    private void sendConfirmation(String paymentMethod) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerId", customerId.value());
        body.put("merchantId", merchantId);
        body.put("addressId", addressId.value());
        body.put("paymentMethodId", paymentMethod);
        body.put("couponCode", couponCode);
        body.put("items", List.copyOf(requestedItems));
        response = RestAssured.given().contentType("application/json").body(body).post("/api/orders/confirm");
    }

    private void putCoupon(String code, String condition) {
        LocalDate today = LocalDate.now(java.time.Clock.systemUTC());
        LocalDate from = today.minusDays(1);
        LocalDate until = today.plusDays(1);
        Money minimum = Money.soles("10.00");
        MerchantId applicableMerchant = new MerchantId(merchantId);
        int maximumUses = 10;
        int uses = 0;
        switch (normalize(condition)) {
            case "expired", "vencido", "vencida", "expirado", "expirada" -> until = today.minusDays(1);
            case "belowminimum", "montominimo", "montominimonocumplido", "minimonocumplido" -> minimum = Money.soles("100.00");
            case "othermerchant", "otrocomercio" -> applicableMerchant = new MerchantId("M-OTHER");
            case "limitreached", "limiteusos", "limitealcanzado", "usosagotados" -> { maximumUses = 1; uses = 1; }
            default -> { }
        }
        Coupon coupon = new Coupon(new CouponCode(code), from, until, minimum, applicableMerchant,
                maximumUses, uses, DiscountType.PERCENTAGE, couponPercent, couponMaximumDiscount);
        marketplace.put(coupon);
        couponCode = code;
    }

    private void storeMerchant(String id, boolean open, CoverageZoneId zone) {
        List<OpeningHours> hours = open
                ? List.of(new OpeningHours(Set.of(DayOfWeek.values()), LocalTime.MIDNIGHT, LocalTime.MAX))
                : List.of();
        marketplace.put(new Merchant(new MerchantId(id), "Comercio EntregaGo", Set.of(zone), hours, 25, true));
    }

    private static boolean isOpen(String state) {
        String value = normalize(state);
        return !(value.contains("cerrad") || value.contains("closed") || value.equals("no") || value.equals("false"));
    }

    private static boolean isCovered(String state) {
        String value = normalize(state);
        return !(value.contains("fuera") || value.equals("no") || value.equals("false")
                || value.contains("sincobertura"));
    }

    private static boolean isApproved(String state) {
        String value = normalize(state);
        if (value.contains("rechaz") || value.contains("deneg") || value.contains("declin")
                || value.contains("noautoriz") || value.contains("noaprob")) return false;
        return value.contains("aprob") || value.contains("autoriz") || value.contains("approved") || value.equals("ok");
    }

    private static Optional<String> optionalTableValue(Map<String, String> row, String... aliases) {
        Map<String, String> normalized = new HashMap<>();
        row.forEach((key, value) -> normalized.put(normalize(key), value));
        for (String alias : aliases) {
            String value = normalized.get(normalize(alias));
            if (value != null && !value.isBlank()) return Optional.of(value.trim());
        }
        for (var entry : normalized.entrySet()) {
            if (Arrays.stream(aliases).anyMatch(alias -> entry.getKey().contains(normalize(alias)))
                    && entry.getValue() != null && !entry.getValue().isBlank()) {
                return Optional.of(entry.getValue().trim());
            }
        }
        return Optional.empty();
    }

    private static String tableValue(Map<String, String> row, String... aliases) {
        return optionalTableValue(row, aliases).orElseThrow(() ->
                new IllegalArgumentException("La DataTable no contiene una columna para " + String.join("/", aliases)));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static Map<String, Object> item(String productId, int quantity, String observations) {
        return Map.of("productId", productId, "quantity", quantity,
                "observations", observations == null ? "" : observations);
    }
}
