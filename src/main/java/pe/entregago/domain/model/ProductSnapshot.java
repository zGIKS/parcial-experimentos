package pe.entregago.domain.model;

import java.math.BigDecimal;

public record ProductSnapshot(
        String id,
        String merchantId,
        String name,
        BigDecimal unitPrice,
        int availableStock,
        boolean available) { }
