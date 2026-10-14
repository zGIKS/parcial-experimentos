package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.aggregates.Order;
import pe.entregago.domain.model.enums.OrderStatus;
import pe.entregago.domain.model.valueobjects.CustomerId;
import pe.entregago.domain.model.valueobjects.RatingId;
import java.time.Instant;

public record Rating(RatingId id, CustomerId customerId, String targetId, int stars, String comment, Instant createdAt) {
    public Rating { if (stars < 1 || stars > 5) throw new IllegalArgumentException("rating must be from 1 to 5"); }
    public boolean isAllowedFor(Order order) { return order.customerId().equals(customerId) && order.status() == OrderStatus.DELIVERED; }
}
