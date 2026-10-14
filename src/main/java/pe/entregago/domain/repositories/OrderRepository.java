package pe.entregago.domain.repositories;
import pe.entregago.domain.model.aggregates.Order;
import pe.entregago.domain.model.valueobjects.OrderId;
import java.util.Optional;
public interface OrderRepository { Order save(Order order); Optional<Order> findById(OrderId id); }
