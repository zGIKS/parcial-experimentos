package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.valueobjects.Money;

public record ProductOption(String name, Money additionalPrice, boolean enabled) {
    public ProductOption { if (name == null || name.isBlank() || additionalPrice == null) throw new IllegalArgumentException("option data required"); }
}
