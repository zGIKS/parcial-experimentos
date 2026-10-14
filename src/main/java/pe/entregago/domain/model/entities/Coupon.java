package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.enums.CouponRejection;
import pe.entregago.domain.model.enums.DiscountType;
import pe.entregago.domain.model.valueobjects.CouponCode;
import pe.entregago.domain.model.valueobjects.CouponDecision;
import pe.entregago.domain.model.valueobjects.MerchantId;
import pe.entregago.domain.model.valueobjects.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class Coupon {
    private final CouponCode code;
    private final LocalDate validFrom, validUntil;
    private final Money minimumSubtotal, maximumDiscount;
    private final MerchantId applicableMerchant;
    private final int maximumUses;
    private int uses;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    public Coupon(CouponCode code, LocalDate validFrom, LocalDate validUntil, Money minimumSubtotal,
                  MerchantId applicableMerchant, int maximumUses, int uses, DiscountType discountType,
                  BigDecimal discountValue, Money maximumDiscount) {
        this.code=code; this.validFrom=validFrom; this.validUntil=validUntil; this.minimumSubtotal=minimumSubtotal;
        this.applicableMerchant=applicableMerchant; this.maximumUses=maximumUses; this.uses=uses;
        this.discountType=discountType; this.discountValue=discountValue; this.maximumDiscount=maximumDiscount;
    }
    public CouponDecision evaluate(MerchantId merchantId, Money subtotal, LocalDate date) {
        CouponRejection rejection = date.isBefore(validFrom) ? CouponRejection.NOT_YET_VALID
                : date.isAfter(validUntil) ? CouponRejection.EXPIRED
                : subtotal.compareTo(minimumSubtotal) < 0 ? CouponRejection.MINIMUM_NOT_MET
                : applicableMerchant != null && !applicableMerchant.equals(merchantId) ? CouponRejection.MERCHANT_NOT_APPLICABLE
                : uses >= maximumUses ? CouponRejection.USAGE_LIMIT_REACHED : null;
        if (rejection != null) return new CouponDecision(Money.zeroPEN(), rejection.name());
        Money discount = discountType == DiscountType.PERCENTAGE
                ? subtotal.multiply(discountValue.movePointLeft(2)) : Money.soles(discountValue);
        return new CouponDecision(discount.min(maximumDiscount).min(subtotal), null);
    }
    public void recordUse() { if (uses >= maximumUses) throw new IllegalStateException("coupon usage limit reached"); uses++; }
    public void undoUse() { if (uses == 0) throw new IllegalStateException("coupon has no use to undo"); uses--; }
    public CouponCode code() { return code; }
    public int uses() { return uses; }
}
