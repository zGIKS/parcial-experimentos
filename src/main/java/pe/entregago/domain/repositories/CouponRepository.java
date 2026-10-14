package pe.entregago.domain.repositories;
import pe.entregago.domain.model.entities.Coupon;
import pe.entregago.domain.model.valueobjects.CouponCode;
import java.util.Optional;
public interface CouponRepository {
    Optional<Coupon> findByCode(CouponCode code);
    void recordUse(CouponCode code);
    void undoUse(CouponCode code);
}
