package pe.entregago.domain.repositories;
import pe.entregago.domain.model.aggregates.Delivery;
import pe.entregago.domain.model.valueobjects.DeliveryId;
import java.util.Optional;
public interface DeliveryRepository { Delivery save(Delivery delivery); Optional<Delivery> findById(DeliveryId id); }
