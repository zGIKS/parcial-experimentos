package pe.entregago.domain.model.valueobjects;

public record AddressId(String value) {
    public AddressId { CustomerId.requireText(value, "addressId"); }
}
