package dev.cristhiansoto.catalog.application.port.in;

import dev.cristhiansoto.catalog.domain.model.Product;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port: what the outside world can ask the catalog to do.
 */
public interface ManageProducts {

    Product create(CreateProductCommand command);

    Product get(UUID id);

    List<Product> list();

    Product update(UUID id, UpdateProductCommand command);

    void delete(UUID id);

    record CreateProductCommand(String name, long priceCents) {}

    record UpdateProductCommand(String name, Long priceCents) {}
}
