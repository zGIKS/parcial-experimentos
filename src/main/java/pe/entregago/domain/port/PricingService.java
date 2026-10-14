package pe.entregago.domain.port;

import pe.entregago.domain.model.PricingLine;
import pe.entregago.domain.model.PriceBreakdown;
import java.util.List;

public interface PricingService {
    PriceBreakdown quote(String merchantId, List<PricingLine> lines, String couponCode);
}
