package dev.cristhiansoto.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductEntity() {}

    ProductEntity(UUID id, String name, long priceCents, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.priceCents = priceCents;
        this.createdAt = createdAt;
    }

    void update(String name, long priceCents) {
        this.name = name;
        this.priceCents = priceCents;
    }

    UUID getId() { return id; }
    String getTenantId() { return tenantId; }
    String getName() { return name; }
    long getPriceCents() { return priceCents; }
    Instant getCreatedAt() { return createdAt; }
}
