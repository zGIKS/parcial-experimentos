package pe.entregago.domain.model;

import pe.entregago.domain.model.enums.BusinessRuleCode;

public final class BusinessRuleViolation extends RuntimeException {
    private final BusinessRuleCode code;
    public BusinessRuleViolation(BusinessRuleCode code, String message) { super(message); this.code = code; }
    public BusinessRuleCode code() { return code; }
}
