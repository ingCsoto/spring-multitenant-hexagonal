package dev.cristhiansoto.catalog.infrastructure.config;

import dev.cristhiansoto.catalog.application.ProductService;
import dev.cristhiansoto.catalog.application.port.in.ManageProducts;
import dev.cristhiansoto.catalog.domain.model.Product;
import dev.cristhiansoto.catalog.domain.port.ProductRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Wires the plain-Java application service and wraps each use case in one transaction, so the
 * application layer stays free of Spring.
 */
@Configuration
class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ManageProducts manageProducts(ProductRepository repository, Clock clock, PlatformTransactionManager txManager) {
        return new TransactionalManageProducts(new ProductService(repository, clock), new TransactionTemplate(txManager));
    }

    private record TransactionalManageProducts(ManageProducts delegate, TransactionTemplate tx) implements ManageProducts {

        @Override
        public Product create(CreateProductCommand command) {
            return tx.execute(status -> delegate.create(command));
        }

        @Override
        public Product get(UUID id) {
            return tx.execute(status -> delegate.get(id));
        }

        @Override
        public List<Product> list() {
            return tx.execute(status -> delegate.list());
        }

        @Override
        public Product update(UUID id, UpdateProductCommand command) {
            return tx.execute(status -> delegate.update(id, command));
        }

        @Override
        public void delete(UUID id) {
            tx.executeWithoutResult(status -> delegate.delete(id));
        }
    }
}
