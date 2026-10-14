package pe.entregago.domain.model.aggregates;

import pe.entregago.domain.model.BusinessRuleViolation;
import pe.entregago.domain.model.entities.OrderLine;
import pe.entregago.domain.model.entities.StockReservation;
import pe.entregago.domain.model.entities.SubstitutionProposal;
import pe.entregago.domain.model.enums.BusinessRuleCode;
import pe.entregago.domain.model.enums.OrderStatus;
import pe.entregago.domain.model.enums.RefundType;
import pe.entregago.domain.model.events.OrderCancelled;
import pe.entregago.domain.model.events.OrderConfirmed;
import pe.entregago.domain.model.events.DomainEvent;
import pe.entregago.domain.model.valueobjects.*;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private final MerchantId merchantId;
    private final AddressId deliveryAddressId;
    private final List<OrderLine> lines;
    private final PriceBreakdown price;
    private final Instant createdAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private final List<SubstitutionProposal> substitutions = new ArrayList<>();
    private OrderStatus status = OrderStatus.CREATED;
    private PaymentAuthorization payment;
    private ReservationId reservationId;
    private Instant confirmedAt;
    private Instant preparationStartedAt;
    private Instant readyAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private RefundType refundType;
    public Order(OrderId id, CustomerId customerId, MerchantId merchantId, AddressId addressId,
                 List<OrderLine> lines, PriceBreakdown price, Instant createdAt) {
        if (lines.isEmpty()) throw new IllegalArgumentException("an order must contain at least one line");
        this.id=id; this.customerId=customerId; this.merchantId=merchantId; this.deliveryAddressId=addressId;
        this.lines=List.copyOf(lines); this.price=price; this.createdAt=createdAt;
    }
    public void confirm(PaymentAuthorization payment, StockReservation reservation, Instant at) {
        if (status != OrderStatus.CREATED) throw invalid("only a created order can be confirmed");
        if (payment == null || !payment.approved()) throw new BusinessRuleViolation(BusinessRuleCode.PAYMENT_NOT_AUTHORIZED, "payment must be authorized");
        if (reservation == null || reservation.status() != pe.entregago.domain.model.enums.ReservationStatus.ACTIVE)
            throw new BusinessRuleViolation(BusinessRuleCode.RESERVATION_NOT_ACTIVE, "active stock reservation is required");
        this.payment=payment; this.reservationId=reservation.id(); this.status=OrderStatus.CONFIRMED; this.confirmedAt=at;
        domainEvents.add(new OrderConfirmed(id, at));
    }
    public void acceptByMerchant() {
        if (substitutions.stream().anyMatch(proposal -> proposal.status() == pe.entregago.domain.model.enums.ApprovalStatus.PENDING))
            throw invalid("merchant cannot accept while a substitution awaits customer approval");
        transition(OrderStatus.CONFIRMED, OrderStatus.MERCHANT_ACCEPTED);
    }
    public void proposeSubstitution(SubstitutionProposal proposal) {
        if (status != OrderStatus.CONFIRMED) throw invalid("substitution can only be proposed before preparation");
        if (!customerId.equals(proposal.customerId())) throw new BusinessRuleViolation(BusinessRuleCode.SUBSTITUTION_NOT_OWNED, "substitution belongs to another order customer");
        substitutions.add(proposal);
    }
    public void approveSubstitution(ProductId original, ProductId replacement, CustomerId actor) {
        substitutions.stream().filter(proposal -> proposal.originalProductId().equals(original)
                && proposal.replacementProductId().equals(replacement)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("substitution proposal was not found")).approve(actor);
    }
    public void rejectSubstitution(ProductId original, ProductId replacement, CustomerId actor) {
        substitutions.stream().filter(proposal -> proposal.originalProductId().equals(original)
                && proposal.replacementProductId().equals(replacement)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("substitution proposal was not found")).reject(actor);
    }
    public void startPreparation(Instant at) { transition(OrderStatus.MERCHANT_ACCEPTED, OrderStatus.IN_PREPARATION); preparationStartedAt=at; }
    public void markReadyForPickup(Instant at) { transition(OrderStatus.IN_PREPARATION, OrderStatus.READY_FOR_PICKUP); readyAt=at; }
    public void markDelivered() { transition(OrderStatus.READY_FOR_PICKUP, OrderStatus.DELIVERED); }
    public void rejectByMerchant(String reason) {
        if (status != OrderStatus.CONFIRMED) throw invalid("only a confirmed order can be rejected by its merchant");
        status=OrderStatus.REJECTED; cancellationReason=reason;
    }
    public RefundType cancel(CancellationPolicy policy, String reason, Instant at) {
        if (status == OrderStatus.CREATED || status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED)
            throw new BusinessRuleViolation(BusinessRuleCode.INVALID_CANCELLATION, "order cannot be cancelled in its current state");
        refundType=policy.refundFor(status, price.total()); status=OrderStatus.CANCELLED; cancellationReason=reason; cancelledAt=at;
        domainEvents.add(new OrderCancelled(id, reason, at)); return refundType;
    }
    public void sendToReview(String reason) {
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) throw invalid("terminal order cannot enter review");
        status=OrderStatus.UNDER_REVIEW; cancellationReason=reason;
    }
    private void transition(OrderStatus expected, OrderStatus next) {
        if (status != expected) throw invalid("expected " + expected + " but order is " + status);
        status=next;
    }
    private BusinessRuleViolation invalid(String message) { return new BusinessRuleViolation(BusinessRuleCode.INVALID_ORDER_TRANSITION, message); }
    public List<DomainEvent> pullDomainEvents() { List<DomainEvent> events=List.copyOf(domainEvents); domainEvents.clear(); return events; }
    public OrderId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public MerchantId merchantId() { return merchantId; }
    public AddressId deliveryAddressId() { return deliveryAddressId; }
    public List<OrderLine> lines() { return lines; }
    public PriceBreakdown price() { return price; }
    public Instant createdAt() { return createdAt; }
    public OrderStatus status() { return status; }
    public PaymentAuthorization payment() { return payment; }
    public ReservationId reservationId() { return reservationId; }
    public Instant confirmedAt() { return confirmedAt; }
    public Instant preparationStartedAt() { return preparationStartedAt; }
    public Instant readyAt() { return readyAt; }
    public Instant cancelledAt() { return cancelledAt; }
    public Optional<Duration> preparationDuration() {
        return preparationStartedAt == null || readyAt == null ? Optional.empty() : Optional.of(Duration.between(preparationStartedAt, readyAt));
    }
    public String cancellationReason() { return cancellationReason; }
    public RefundType refundType() { return refundType; }
    public List<SubstitutionProposal> substitutions() { return List.copyOf(substitutions); }
}
