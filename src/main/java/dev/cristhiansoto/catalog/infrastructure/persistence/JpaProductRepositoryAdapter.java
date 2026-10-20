package dev.cristhiansoto.catalog.infrastructure.persistence;

import dev.cristhiansoto.catalog.domain.model.Money;
import dev.cristhiansoto.catalog.domain.model.Product;
import dev.cristhiansoto.catalog.domain.port.ProductRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbound adapter. Reads and updates always go through tenant-scoped queries; new rows are persisted
 * and stamped with the current tenant by Hibernate.
 */
@Repository
@Transactional
class JpaProductRepositoryAdapter implements ProductRepository {

    private final SpringDataProductRepository jpa;
    private final EntityManager entityManager;

    JpaProductRepositoryAdapter(SpringDataProductRepository jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = jpa.findScopedById(product.id())
                .map(existing -> {
                    existing.update(product.name(), product.price().cents());
                    return existing;
                })
                .orElseGet(() -> {
                    ProductEntity created = new ProductEntity(
                            product.id(), product.name(), product.price().cents(), product.createdAt());
                    entityManager.persist(created);
                    return created;
                });
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(UUID id) {
        return jpa.findScopedById(id).map(JpaProductRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return jpa.findAllByOrderByCreatedAtAscNameAsc().stream().map(JpaProductRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    public boolean deleteById(UUID id) {
        return jpa.findScopedById(id)
                .map(entity -> {
                    jpa.delete(entity);
                    return true;
                })
                .orElse(false);
    }

    private static Product toDomain(ProductEntity e) {
        return Product.restore(e.getId(), e.getName(), Money.ofCents(e.getPriceCents()), e.getCreatedAt());
    }
}
