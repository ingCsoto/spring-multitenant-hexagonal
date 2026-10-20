package dev.cristhiansoto.catalog.infrastructure.web;

import dev.cristhiansoto.catalog.application.port.in.ManageProducts;
import dev.cristhiansoto.catalog.application.port.in.ManageProducts.CreateProductCommand;
import dev.cristhiansoto.catalog.application.port.in.ManageProducts.UpdateProductCommand;
import dev.cristhiansoto.catalog.domain.model.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ManageProducts products;

    ProductController(ManageProducts products) {
        this.products = products;
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest body) {
        Product created = products.create(new CreateProductCommand(body.name(), body.priceCents()));
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(ProductResponse.from(created));
    }

    @GetMapping
    List<ProductResponse> list() {
        return products.list().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(products.get(id));
    }

    @PutMapping("/{id}")
    ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest body) {
        return ProductResponse.from(products.update(id, new UpdateProductCommand(body.name(), body.priceCents())));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        products.delete(id);
        return ResponseEntity.noContent().build();
    }

    record CreateProductRequest(
            @NotBlank @Size(max = Product.MAX_NAME_LENGTH) String name,
            @NotNull @PositiveOrZero Long priceCents) {}

    record UpdateProductRequest(
            @Size(min = 1, max = Product.MAX_NAME_LENGTH) String name,
            @PositiveOrZero Long priceCents) {}

    record ProductResponse(UUID id, String name, long priceCents, Instant createdAt) {
        static ProductResponse from(Product p) {
            return new ProductResponse(p.id(), p.name(), p.price().cents(), p.createdAt());
        }
    }
}
