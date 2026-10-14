package pe.entregago.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.entregago.domain.model.AddressSnapshot;
import pe.entregago.domain.model.FailureReason;
import pe.entregago.domain.model.MerchantSnapshot;
import pe.entregago.domain.model.OrderConfirmationCommand;
import pe.entregago.domain.model.OrderRejectedException;
import pe.entregago.domain.model.PaymentAuthorization;
import pe.entregago.domain.model.PriceBreakdown;
import pe.entregago.domain.model.ProductSnapshot;
import pe.entregago.domain.model.RequestedItem;
import pe.entregago.domain.model.ReservationToken;
import pe.entregago.domain.port.AddressRepository;
import pe.entregago.domain.port.CoveragePolicy;
import pe.entregago.domain.port.InventoryPort;
import pe.entregago.domain.port.MerchantRepository;
import pe.entregago.domain.port.OrderRepository;
import pe.entregago.domain.port.PaymentGateway;
import pe.entregago.domain.port.PricingService;
import pe.entregago.domain.port.ProductCatalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderConfirmationServiceTest {
    private static final String CUSTOMER = "C-01";
    private static final String MERCHANT = "M-01";
    private static final String ADDRESS = "A-01";
    private static final String PRODUCT = "P-01";
    private static final OrderConfirmationCommand COMMAND = new OrderConfirmationCommand(
            CUSTOMER, MERCHANT, ADDRESS, "PM-01", null, List.of(new RequestedItem(PRODUCT, 2)));

    @Mock MerchantRepository merchants;
    @Mock AddressRepository addresses;
    @Mock ProductCatalog catalog;
    @Mock CoveragePolicy coverage;
    @Mock InventoryPort inventory;
    @Mock PricingService pricing;
    @Mock PaymentGateway payments;
    @Mock OrderRepository orders;
    @InjectMocks OrderConfirmationService service;

    @BeforeEach
    void arrangeEligibleOrder() {
        when(merchants.findMerchant(MERCHANT)).thenReturn(Optional.of(new MerchantSnapshot(MERCHANT, true, "Z-01")));
        when(addresses.findAddress(ADDRESS)).thenReturn(Optional.of(new AddressSnapshot(ADDRESS, CUSTOMER, "Z-01")));
        when(coverage.covers(any(MerchantSnapshot.class), any(AddressSnapshot.class))).thenReturn(true);
        when(catalog.findProduct(PRODUCT)).thenReturn(Optional.of(
                new ProductSnapshot(PRODUCT, MERCHANT, "Producto de prueba", new BigDecimal("10.00"), 5, true)));
        when(pricing.quote(anyString(), anyList(), any())).thenReturn(
                new PriceBreakdown(new BigDecimal("20.00"), new BigDecimal("0.00"),
                        new BigDecimal("4.00"), new BigDecimal("2.00"), new BigDecimal("26.00"), null, null));
    }

    @Test
    void shouldRejectClosedMerchantBeforeReservingStockOrAuthorizingPayment() {
        when(merchants.findMerchant(MERCHANT)).thenReturn(Optional.of(new MerchantSnapshot(MERCHANT, false, "Z-01")));

        assertThatThrownBy(() -> service.confirm(COMMAND))
                .isInstanceOfSatisfying(OrderRejectedException.class,
                        error -> assertThat(error.reason()).isEqualTo(FailureReason.MERCHANT_CLOSED));

        verifyNoInteractions(addresses, catalog, coverage, inventory, pricing, payments, orders);
    }

    @Test
    void shouldRejectWhenAtomicReservationFindsInsufficientStockWithoutCharging() {
        when(inventory.reserveAll(anyList())).thenThrow(new OrderRejectedException(FailureReason.INSUFFICIENT_STOCK));

        assertThatThrownBy(() -> service.confirm(COMMAND))
                .isInstanceOfSatisfying(OrderRejectedException.class,
                        error -> assertThat(error.reason()).isEqualTo(FailureReason.INSUFFICIENT_STOCK));

        verify(payments, never()).authorize(anyString(), any(), anyString());
        verify(orders, never()).saveConfirmed(any());
    }

    @Test
    void shouldReleaseReservationWhenPaymentAuthorizationIsRejected() {
        ReservationToken reservation = new ReservationToken("R-01");
        when(inventory.reserveAll(anyList())).thenReturn(reservation);
        when(payments.authorize(anyString(), any(), anyString())).thenReturn(
                new PaymentAuthorization("PAY-DECLINED", false));

        assertThatThrownBy(() -> service.confirm(COMMAND))
                .isInstanceOfSatisfying(OrderRejectedException.class,
                        error -> assertThat(error.reason()).isEqualTo(FailureReason.PAYMENT_NOT_AUTHORIZED));

        verify(inventory).release(reservation);
        verify(orders, never()).saveConfirmed(any());
    }
}
