package dev.cristhiansoto.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A product in a tenant's catalog.
 *
 * <p>The domain does not know about tenants on purpose: isolation is enforced at the
 * infrastructure boundary, so no use case can forget it.
 */
public final class Product {

    public static final int MAX_NAME_LENGTH = 120;

    private final UUID id;
    private final String name;
    private final Money price;
    private final Instant createdAt;

    private Product(UUID id, String name, Money price, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = validateName(name);
        this.price = Objects.requireNonNull(price, "price");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Product create(String name, Money price, Instant now) {
        return new Product(UUID.randomUUID(), name, price, now);
    }

    public static Product restore(UUID id, String name, Money price, Instant createdAt) {
        return new Product(id, name, price, createdAt);
    }

    public Product rename(String newName) {
        return new Product(id, newName, price, createdAt);
    }

    public Product reprice(Money newPrice) {
        return new Product(id, name, newPrice, createdAt);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidProductException("Product name must not be blank");
        }
        String trimmed = name.strip();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidProductException("Product name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public Money price() { return price; }
    public Instant createdAt() { return createdAt; }
}
