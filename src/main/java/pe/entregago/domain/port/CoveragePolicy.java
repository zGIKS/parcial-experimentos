package pe.entregago.domain.port;

import pe.entregago.domain.model.AddressSnapshot;
import pe.entregago.domain.model.MerchantSnapshot;

public interface CoveragePolicy {
    boolean covers(MerchantSnapshot merchant, AddressSnapshot address);
}
