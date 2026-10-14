package pe.entregago.domain.model.entities;

import pe.entregago.domain.model.valueobjects.AddressId;
import pe.entregago.domain.model.valueobjects.CoverageZoneId;
import pe.entregago.domain.model.valueobjects.CustomerId;
import pe.entregago.domain.model.valueobjects.GeoLocation;

public record CustomerAddress(AddressId id, CustomerId customerId, String label,
                              String streetAddress, CoverageZoneId zoneId, GeoLocation location) {
    public CustomerAddress {
        if (id == null || customerId == null || zoneId == null || location == null
                || label == null || label.isBlank() || streetAddress == null || streetAddress.isBlank())
            throw new IllegalArgumentException("address details are required");
    }
}
