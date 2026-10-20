package dev.cristhiansoto.catalog.domain.model;

/**
 * An amount in minor units (cents), never negative. Stored as a long to avoid floating-point errors.
 */
public record Money(long cents) {

    public Money {
        if (cents < 0) {
            throw new InvalidProductException("Price must not be negative");
        }
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }
}
