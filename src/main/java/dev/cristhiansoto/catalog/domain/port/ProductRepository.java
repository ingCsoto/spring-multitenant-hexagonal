package dev.cristhiansoto.catalog.domain.port;

import dev.cristhiansoto.catalog.domain.model.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port. Implementations must scope every operation to the current tenant.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAll();

    boolean existsByName(String name);

    boolean deleteById(UUID id);
}
