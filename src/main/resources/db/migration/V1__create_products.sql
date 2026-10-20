CREATE TABLE products (
    id          UUID         PRIMARY KEY,
    tenant_id   VARCHAR(63)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    price_cents BIGINT       NOT NULL CHECK (price_cents >= 0),
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_products_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX ix_products_tenant_created ON products (tenant_id, created_at);
