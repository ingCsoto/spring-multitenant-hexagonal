package dev.cristhiansoto.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataProductRepository extends JpaRepository<ProductEntity, UUID> {

    /** A query, not {@code findById}: queries are the path where Hibernate applies the tenant restriction. */
    @Query("select p from ProductEntity p where p.id = :id")
    Optional<ProductEntity> findScopedById(@Param("id") UUID id);

    List<ProductEntity> findAllByOrderByCreatedAtAscNameAsc();

    boolean existsByName(String name);
}
