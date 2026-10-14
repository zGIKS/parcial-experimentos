package pe.entregago.domain.port;

import pe.entregago.domain.model.ProductSnapshot;
import java.util.Optional;

public interface ProductCatalog {
    Optional<ProductSnapshot> findProduct(String productId);
}
