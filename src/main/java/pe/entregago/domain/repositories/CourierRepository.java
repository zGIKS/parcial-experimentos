package pe.entregago.domain.repositories;
import pe.entregago.domain.model.aggregates.Order;
import pe.entregago.domain.model.entities.Courier;
import java.util.List;
public interface CourierRepository { List<Courier> findEligible(Order order, int requiredCapacity); }
