package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.enums.ApprovalStatus;
import pe.entregago.domain.model.valueobjects.CustomerId;
import pe.entregago.domain.model.valueobjects.ProductId;
import java.time.Instant;

public final class SubstitutionProposal {
    private final ProductId originalProductId, replacementProductId;
    private final CustomerId customerId;
    private final Instant proposedAt;
    private ApprovalStatus status=ApprovalStatus.PENDING;
    public SubstitutionProposal(ProductId originalProductId, ProductId replacementProductId, CustomerId customerId, Instant proposedAt) {
        if (originalProductId.equals(replacementProductId)) throw new IllegalArgumentException("replacement must differ from original product");
        this.originalProductId=originalProductId; this.replacementProductId=replacementProductId;
        this.customerId=customerId; this.proposedAt=proposedAt;
    }
    public void approve(CustomerId customerId) { requireCustomer(customerId); requirePending(); status=ApprovalStatus.APPROVED; }
    public void reject(CustomerId customerId) { requireCustomer(customerId); requirePending(); status=ApprovalStatus.REJECTED; }
    private void requireCustomer(CustomerId actorId) {
        if (!customerId.equals(actorId)) throw new pe.entregago.domain.model.BusinessRuleViolation(
                pe.entregago.domain.model.enums.BusinessRuleCode.SUBSTITUTION_NOT_OWNED,
                "only the customer who placed the order can decide on this substitution");
    }
    private void requirePending() { if (status != ApprovalStatus.PENDING) throw new IllegalStateException("proposal already decided"); }
    public ProductId originalProductId() { return originalProductId; }
    public ProductId replacementProductId() { return replacementProductId; }
    public CustomerId customerId() { return customerId; }
    public Instant proposedAt() { return proposedAt; }
    public ApprovalStatus status() { return status; }
}
