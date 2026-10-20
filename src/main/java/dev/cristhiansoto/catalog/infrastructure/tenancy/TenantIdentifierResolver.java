package dev.cristhiansoto.catalog.infrastructure.tenancy;

import java.util.Map;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which tenant the current session belongs to. Every query on an entity with
 * {@code @TenantId} is restricted to it, and every insert is stamped with it.
 *
 * <p>Fail-closed: with no tenant in context it returns a value no row can have, so a code path that
 * forgets to set the tenant reads nothing instead of everything.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    static final String NO_TENANT = "__no_tenant__";

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.get().orElse(NO_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
