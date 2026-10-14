package pe.entregago.domain.model;

import java.math.BigDecimal;

public record PricingLine(String productId, BigDecimal unitPrice, int quantity) { }
