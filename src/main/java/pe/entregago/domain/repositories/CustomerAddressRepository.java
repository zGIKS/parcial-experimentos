package pe.entregago.domain.repositories;
import pe.entregago.domain.model.entities.CustomerAddress;
import pe.entregago.domain.model.valueobjects.AddressId;
import java.util.Optional;
public interface CustomerAddressRepository { Optional<CustomerAddress> findById(AddressId id); }
