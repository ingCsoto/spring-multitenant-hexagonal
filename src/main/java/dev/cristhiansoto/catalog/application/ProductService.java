package dev.cristhiansoto.catalog.application;

import dev.cristhiansoto.catalog.application.port.in.ManageProducts;
import dev.cristhiansoto.catalog.domain.model.DuplicateProductException;
import dev.cristhiansoto.catalog.domain.model.Money;
import dev.cristhiansoto.catalog.domain.model.Product;
import dev.cristhiansoto.catalog.domain.model.ProductNotFoundException;
import dev.cristhiansoto.catalog.domain.port.ProductRepository;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service. Plain Java: wired by the infrastructure, testable without Spring.
 */
public class ProductService implements ManageProducts {

    private final ProductRepository products;
    private final Clock clock;

    public ProductService(ProductRepository products, Clock clock) {
        this.products = Objects.requireNonNull(products);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Product create(CreateProductCommand command) {
        Product product = Product.create(command.name(), Money.ofCents(command.priceCents()), clock.instant());
        if (products.existsByName(product.name())) {
            throw new DuplicateProductException(product.name());
        }
        return products.save(product);
    }

    @Override
    public Product get(UUID id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public List<Product> list() {
        return products.findAll();
    }

    @Override
    public Product update(UUID id, UpdateProductCommand command) {
        Product current = get(id);
        Product updated = current;
        if (command.name() != null && !command.name().strip().equals(current.name())) {
            updated = updated.rename(command.name());
            if (products.existsByName(updated.name())) {
                throw new DuplicateProductException(updated.name());
            }
        }
        if (command.priceCents() != null) {
            updated = updated.reprice(Money.ofCents(command.priceCents()));
        }
        return products.save(updated);
    }

    @Override
    public void delete(UUID id) {
        if (!products.deleteById(id)) {
            throw new ProductNotFoundException(id);
        }
    }
}
