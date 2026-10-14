package pe.entregago.domain.repositories;
import pe.entregago.domain.model.entities.Product;
import pe.entregago.domain.model.valueobjects.ProductId;
import java.util.Optional;
public interface ProductRepository { Optional<Product> findById(ProductId id); }
