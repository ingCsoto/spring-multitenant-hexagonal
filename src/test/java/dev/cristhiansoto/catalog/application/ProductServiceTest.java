package dev.cristhiansoto.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.cristhiansoto.catalog.application.port.in.ManageProducts.CreateProductCommand;
import dev.cristhiansoto.catalog.application.port.in.ManageProducts.UpdateProductCommand;
import dev.cristhiansoto.catalog.domain.model.DuplicateProductException;
import dev.cristhiansoto.catalog.domain.model.Product;
import dev.cristhiansoto.catalog.domain.model.ProductNotFoundException;
import dev.cristhiansoto.catalog.domain.port.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private final InMemoryProducts repo = new InMemoryProducts();
    private final ProductService service =
            new ProductService(repo, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void createsAProductWithTheClockTime() {
        Product p = service.create(new CreateProductCommand("Coffee", 1500));
        assertThat(p.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(service.list()).extracting(Product::name).containsExactly("Coffee");
    }

    @Test
    void rejectsADuplicateName() {
        service.create(new CreateProductCommand("Coffee", 1500));
        assertThatThrownBy(() -> service.create(new CreateProductCommand(" Coffee ", 10)))
                .isInstanceOf(DuplicateProductException.class);
    }

    @Test
    void updatesOnlyTheFieldsThatAreSent() {
        Product p = service.create(new CreateProductCommand("Coffee", 1500));
        Product updated = service.update(p.id(), new UpdateProductCommand(null, 1800L));
        assertThat(updated.name()).isEqualTo("Coffee");
        assertThat(updated.price().cents()).isEqualTo(1800);
    }

    @Test
    void renamingToAnExistingNameIsRejected() {
        service.create(new CreateProductCommand("Coffee", 1500));
        Product tea = service.create(new CreateProductCommand("Tea", 900));
        assertThatThrownBy(() -> service.update(tea.id(), new UpdateProductCommand("Coffee", null)))
                .isInstanceOf(DuplicateProductException.class);
    }

    @Test
    void missingProductsAreReportedAsNotFound() {
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.delete(unknown)).isInstanceOf(ProductNotFoundException.class);
    }

    private static final class InMemoryProducts implements ProductRepository {
        private final Map<UUID, Product> rows = new LinkedHashMap<>();

        @Override public Product save(Product product) { rows.put(product.id(), product); return product; }
        @Override public Optional<Product> findById(UUID id) { return Optional.ofNullable(rows.get(id)); }
        @Override public List<Product> findAll() { return new ArrayList<>(rows.values()); }
        @Override public boolean existsByName(String name) { return rows.values().stream().anyMatch(p -> p.name().equals(name)); }
        @Override public boolean deleteById(UUID id) { return rows.remove(id) != null; }
    }
}
