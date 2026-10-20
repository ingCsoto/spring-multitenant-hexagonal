package dev.cristhiansoto.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.cristhiansoto.catalog.domain.model.InvalidProductException;
import dev.cristhiansoto.catalog.domain.model.Money;
import dev.cristhiansoto.catalog.domain.model.Product;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void trimsTheName() {
        assertThat(Product.create("  Coffee  ", Money.ofCents(1500), NOW).name()).isEqualTo("Coffee");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Product.create("   ", Money.ofCents(1), NOW)).isInstanceOf(InvalidProductException.class);
    }

    @Test
    void rejectsNameLongerThanTheLimit() {
        String tooLong = "x".repeat(Product.MAX_NAME_LENGTH + 1);
        assertThatThrownBy(() -> Product.create(tooLong, Money.ofCents(1), NOW)).isInstanceOf(InvalidProductException.class);
    }

    @Test
    void acceptsNameExactlyAtTheLimit() {
        String atLimit = "x".repeat(Product.MAX_NAME_LENGTH);
        assertThat(Product.create(atLimit, Money.ofCents(1), NOW).name()).hasSize(Product.MAX_NAME_LENGTH);
    }

    @Test
    void rejectsNegativePriceButAcceptsZero() {
        assertThatThrownBy(() -> Money.ofCents(-1)).isInstanceOf(InvalidProductException.class);
        assertThat(Money.ofCents(0).cents()).isZero();
    }

    @Test
    void renameAndRepriceKeepIdentity() {
        Product original = Product.create("Tea", Money.ofCents(900), NOW);
        Product changed = original.rename("Green tea").reprice(Money.ofCents(1100));
        assertThat(changed.id()).isEqualTo(original.id());
        assertThat(changed.createdAt()).isEqualTo(original.createdAt());
        assertThat(changed.name()).isEqualTo("Green tea");
        assertThat(changed.price().cents()).isEqualTo(1100);
    }
}
