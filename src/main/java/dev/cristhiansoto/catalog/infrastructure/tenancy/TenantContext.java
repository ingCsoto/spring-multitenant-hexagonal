package dev.cristhiansoto.catalog.infrastructure.tenancy;

import java.util.Optional;

/**
 * Holds the tenant of the current request. Set by {@code TenantFilter}, always cleared in a finally block.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<String> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
