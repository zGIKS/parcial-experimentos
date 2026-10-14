package pe.entregago.domain.port;

import pe.entregago.domain.model.AddressSnapshot;
import java.util.Optional;

public interface AddressRepository {
    Optional<AddressSnapshot> findAddress(String addressId);
}
