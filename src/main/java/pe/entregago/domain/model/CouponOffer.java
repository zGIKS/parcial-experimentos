package pe.entregago.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CouponOffer(
        String code,
        String merchantId,
        LocalDate validFrom,
        LocalDate validUntil,
        BigDecimal minimumSubtotal,
        BigDecimal percentage,
        BigDecimal maximumDiscount,
        int maximumUses,
        int uses) { }
