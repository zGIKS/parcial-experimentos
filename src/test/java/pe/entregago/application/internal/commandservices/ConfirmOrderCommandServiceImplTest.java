package pe.entregago.application.internal.commandservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.entregago.domain.model.BusinessRuleViolation;
import pe.entregago.domain.model.aggregates.Merchant;
import pe.entregago.domain.model.entities.*;
import pe.entregago.domain.model.enums.BusinessRuleCode;
import pe.entregago.domain.model.commands.ConfirmOrderCommand;
import pe.entregago.domain.model.valueobjects.*;
import pe.entregago.domain.repositories.*;
import pe.entregago.domain.services.OrderPricingPolicy;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmOrderCommandServiceImplTest {
    private static final Instant NOW=Instant.parse("2026-10-14T17:00:00Z");
    private static final CustomerId CUSTOMER=new CustomerId("C-1");
    private static final MerchantId MERCHANT=new MerchantId("M-1");
    private static final AddressId ADDRESS=new AddressId("A-1");
    private static final ProductId PRODUCT=new ProductId("P-1");
    private static final CoverageZoneId ZONE=new CoverageZoneId("Z-CENTRO");
    @Mock MerchantRepository merchants;
    @Mock CustomerAddressRepository addresses;
    @Mock ProductRepository products;
    @Mock CouponRepository coupons;
    @Mock StockReservationRepository reservations;
    @Mock PaymentGateway payments;
    @Mock OrderRepository orders;
    @Mock OrderPricingPolicy pricing;
    private ConfirmOrderCommandServiceImpl service;
    private ConfirmOrderCommand command;

    @BeforeEach void setUp() {
        service=new ConfirmOrderCommandServiceImpl(merchants, addresses, products, coupons, reservations,
                payments, orders, pricing, Clock.fixed(NOW, ZoneOffset.UTC));
        command=new ConfirmOrderCommand(CUSTOMER, MERCHANT, ADDRESS, new PaymentMethodId("CARD-1"), null,
                List.of(new RequestedItem(PRODUCT, new Quantity(2), "sin cebolla")));
    }

    @Test void closedMerchantPreventsReservationAndPaymentAuthorization() {
        when(merchants.findById(MERCHANT)).thenReturn(Optional.of(merchant(false, Set.of(ZONE))));

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOfSatisfying(BusinessRuleViolation.class,
                        failure -> assertThat(failure.code()).isEqualTo(BusinessRuleCode.MERCHANT_CLOSED));
        verifyNoInteractions(addresses, products, coupons, reservations, payments, orders, pricing);
    }

    @Test void atomicStockReservationRejectsInsufficientInventoryBeforePayment() {
        givenEligibleMerchantAndProduct();
        when(pricing.quote(eq(MERCHANT), anyList(), isNull(), any(LocalDate.class))).thenReturn(price());
        when(reservations.reserveAll(any(), eq(command.items()), eq(NOW)))
                .thenThrow(new BusinessRuleViolation(BusinessRuleCode.INSUFFICIENT_STOCK, "insufficient stock"));

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOfSatisfying(BusinessRuleViolation.class,
                        failure -> assertThat(failure.code()).isEqualTo(BusinessRuleCode.INSUFFICIENT_STOCK));
        verify(payments, never()).authorize(any(), any(), any());
        verify(orders, never()).save(any());
    }

    @Test void rejectedPaymentReleasesReservationAndDoesNotPersistConfirmedOrder() {
        givenEligibleMerchantAndProduct();
        when(pricing.quote(eq(MERCHANT), anyList(), isNull(), any(LocalDate.class))).thenReturn(price());
        when(reservations.reserveAll(any(), eq(command.items()), eq(NOW))).thenAnswer(invocation -> {
            ReservationId generatedId=invocation.getArgument(0);
            return new StockReservation(generatedId, java.util.Map.of(PRODUCT, 2), NOW);
        });
        when(payments.authorize(any(), any(), any())).thenReturn(new PaymentAuthorization("DECLINED-1", false));

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOfSatisfying(BusinessRuleViolation.class,
                        failure -> assertThat(failure.code()).isEqualTo(BusinessRuleCode.PAYMENT_NOT_AUTHORIZED));
        ArgumentCaptor<ReservationId> reservationIdCaptor=ArgumentCaptor.forClass(ReservationId.class);
        verify(reservations).reserveAll(reservationIdCaptor.capture(), eq(command.items()), eq(NOW));
        verify(reservations).release(reservationIdCaptor.getValue());
        verify(payments, never()).voidAuthorization(any());
        verify(orders, never()).save(any());
    }

    private void givenEligibleMerchantAndProduct() {
        when(merchants.findById(MERCHANT)).thenReturn(Optional.of(merchant(true, Set.of(ZONE))));
        when(addresses.findById(ADDRESS)).thenReturn(Optional.of(new CustomerAddress(ADDRESS, CUSTOMER,
                "Casa", "Av. Lima 123", ZONE, new GeoLocation(-12.04, -77.03))));
        when(products.findById(PRODUCT)).thenReturn(Optional.of(new Product(PRODUCT, MERCHANT, "Hamburguesa",
                "Comida", Money.soles("10.00"), 5, true)));
    }
    private static Merchant merchant(boolean open, Set<CoverageZoneId> zones) {
        List<OpeningHours> hours=open ? List.of(new OpeningHours(Set.of(DayOfWeek.values()), LocalTime.MIDNIGHT, LocalTime.of(23,59))) : List.of();
        return new Merchant(MERCHANT, "Comercio", zones, hours, 20, true);
    }
    private static PriceBreakdown price() {
        return new PriceBreakdown(Money.soles("20.00"), Money.zeroPEN(), Money.soles("4.00"), Money.soles("2.00"), Money.soles("26.00"), null, null);
    }
}
