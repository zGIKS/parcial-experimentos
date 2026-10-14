package pe.entregago.domain.port;

import pe.entregago.domain.model.MerchantSnapshot;
import java.util.Optional;

public interface MerchantRepository {
    Optional<MerchantSnapshot> findMerchant(String merchantId);
}
