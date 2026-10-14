package pe.entregago.domain.model.aggregates;

import pe.entregago.domain.model.entities.CustomerAddress;
import pe.entregago.domain.model.valueobjects.CustomerId;
import java.util.ArrayList;
import java.util.List;

public final class Customer {
    private final CustomerId id;
    private final String fullName;
    private final String email;
    private final List<CustomerAddress> addresses = new ArrayList<>();
    public Customer(CustomerId id, String fullName, String email) { this.id=id; this.fullName=fullName; this.email=email; }
    public void addAddress(CustomerAddress address) {
        if (!id.equals(address.customerId())) throw new IllegalArgumentException("address owner differs from customer");
        addresses.removeIf(existing -> existing.id().equals(address.id())); addresses.add(address);
    }
    public CustomerId id() { return id; }
    public String fullName() { return fullName; }
    public String email() { return email; }
    public List<CustomerAddress> addresses() { return List.copyOf(addresses); }
}
