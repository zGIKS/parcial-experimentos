package pe.entregago.domain.repositories;
import pe.entregago.domain.model.aggregates.Merchant;
import pe.entregago.domain.model.valueobjects.MerchantId;
import java.util.Optional;
public interface MerchantRepository { Optional<Merchant> findById(MerchantId id); }
