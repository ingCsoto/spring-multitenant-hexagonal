# spring-multitenant-hexagonal

[![ci](https://github.com/ingCsoto/spring-multitenant-hexagonal/actions/workflows/ci.yml/badge.svg)](https://github.com/ingCsoto/spring-multitenant-hexagonal/actions/workflows/ci.yml)

A multi-tenant product catalog API in **Java 21 and Spring Boot 3.5**. It shows two things done properly:

- **Hexagonal architecture.** The domain and the use cases are plain Java. Spring, JPA and HTTP live only in adapters. An ArchUnit test fails the build if that rule is broken.
- **Tenant isolation that cannot be forgotten.** The tenant comes from a Keycloak JWT claim. Hibernate's `@TenantId` applies it to every query and every insert. A request with no valid tenant is rejected, and code that runs without a tenant reads nothing.

## Architecture

```mermaid
flowchart LR
    client([Client]) -->|Bearer JWT| sec

    subgraph infrastructure
        sec[SecurityConfig<br/>JWT + roles] --> tf[TenantFilter<br/>tenant_id claim]
        tf --> web[ProductController]
        repo[JpaProductRepositoryAdapter<br/>@TenantId entity]
    end

    subgraph application
        svc[ProductService<br/>use cases]
    end

    subgraph domain
        model[Product · Money]
        port[[ProductRepository port]]
    end

    web -->|ManageProducts port| svc
    svc --> model
    svc --> port
    repo -. implements .-> port
    repo --> db[(PostgreSQL)]
    kc[(Keycloak)] -. issues JWT .-> client
```

Dependencies point inwards only: `infrastructure → application → domain`.

## How tenant isolation works

1. **Authentication.** Spring Security validates the JWT signature and issuer against Keycloak. Realm roles become authorities.
2. **Tenant resolution.** `TenantFilter` reads the `tenant_id` claim and checks its format. It puts the tenant in `TenantContext` for the request and always clears it afterwards. If an authenticated token has no valid tenant, the response is **403**; there is no default tenant.
3. **Enforcement in the data layer.** `ProductEntity.tenantId` carries `@TenantId`. Hibernate adds `tenant_id = :current` to every query on the entity and stamps the tenant on every insert. Reads and updates go through queries, never through `find` by id, so the restriction always applies.
4. **Fail-closed default.** With no tenant in context, the resolver returns a value no row can have. A forgotten code path reads nothing instead of everything.

The domain does not know about tenants at all. Isolation is a property of the infrastructure boundary, so a new use case cannot skip it.

**Why a discriminator column.** The alternatives are schema-per-tenant and database-per-tenant. A shared table with a `tenant_id` column keeps one migration path and one connection pool, and it scales to many small tenants. The cost is that isolation depends on the application. That is exactly why it is enforced in one place, Hibernate, and covered by tests.

## Security model

| Request | Result |
|---|---|
| No token or an invalid signature | 401 |
| Valid token without a `tenant_id` claim | 403 |
| Any authenticated user of a tenant | Can read that tenant's products |
| `catalog-writer` role | Can create, update and delete in its own tenant |
| Another tenant's product id | 404, never 403, so ids from other tenants are not revealed |

## Run it locally

Requirements: JDK 21, Docker.

```bash
docker compose up -d   # PostgreSQL on :5432, Keycloak on :8180 with the demo realm
./mvnw spring-boot:run
```

The demo realm is for local use only. All users have the password `password`.

| User | Tenant | Role |
|---|---|---|
| alice | acme | catalog-writer |
| bob | globex | catalog-writer |
| carol | acme | reader only |

Get a token and use it:

```bash
token() {
  curl -s -d grant_type=password -d client_id=catalog-cli -d username=$1 -d password=password \
    http://localhost:8180/realms/catalog/protocol/openid-connect/token | jq -r .access_token
}

ALICE=$(token alice); BOB=$(token bob)

curl -s -X POST localhost:8080/api/products -H "Authorization: Bearer $ALICE" \
  -H 'Content-Type: application/json' -d '{"name":"Coffee","priceCents":1500}'

curl -s localhost:8080/api/products -H "Authorization: Bearer $ALICE"   # Alice sees her product
curl -s localhost:8080/api/products -H "Authorization: Bearer $BOB"     # Bob sees []
```

## API

| Method | Path | Role |
|---|---|---|
| `GET` | `/api/products` | any authenticated user |
| `GET` | `/api/products/{id}` | any authenticated user |
| `POST` | `/api/products` | `catalog-writer` |
| `PUT` | `/api/products/{id}` | `catalog-writer` |
| `DELETE` | `/api/products/{id}` | `catalog-writer` |

Errors use RFC 9457 problem details (`application/problem+json`). Internal details never reach the client.

## Tests

```bash
mvn verify
```

| Suite | What it proves |
|---|---|
| `ProductTest` | Domain invariants: name length, blank names, negative prices. |
| `ProductServiceTest` | Use cases with an in-memory repository, without Spring. |
| `HexagonalArchitectureTest` | The domain and application layers do not depend on Spring, JPA or the infrastructure. |
| `ProductApiIntegrationTest` | The full HTTP flow against a real PostgreSQL in Testcontainers: isolation between tenants, roles, 401/403/404/409 and validation. |

The isolation tests were checked against a mutant. With `@TenantId` removed, `aTenantOnlySeesItsOwnProducts` and `aTenantCannotUpdateOrDeleteAnotherTenantsProduct` fail. A test that cannot fail proves nothing.

## Project structure

```
src/main/java/dev/cristhiansoto/catalog
├── domain            Product, Money, exceptions, ProductRepository port (plain Java)
├── application       ProductService and the ManageProducts inbound port (plain Java)
└── infrastructure
    ├── web           REST controller, problem-detail error mapping
    ├── security      JWT and Keycloak roles, TenantFilter
    ├── tenancy       TenantContext and the Hibernate tenant resolver
    ├── persistence   JPA entity with @TenantId and the repository adapter
    └── config        Wiring and one transaction per use case
```

## Stack

Java 21 · Spring Boot 3.5 · Spring Security OAuth2 Resource Server · Spring Data JPA / Hibernate 6 · Flyway · PostgreSQL 16 · Keycloak 26 · JUnit 5 · Testcontainers · ArchUnit · GitHub Actions

## License

MIT
