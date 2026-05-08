CREATE TABLE products (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          sku             VARCHAR(50) NOT NULL UNIQUE,
                          name            VARCHAR(200) NOT NULL,
                          description     TEXT,
                          price           DECIMAL(12,2) NOT NULL CHECK (price >= 0),
                          stock_current   INTEGER NOT NULL DEFAULT 0 CHECK (stock_current >= 0),
                          stock_minimum   INTEGER NOT NULL DEFAULT 10 CHECK (stock_minimum >= 0),
                          active          BOOLEAN NOT NULL DEFAULT TRUE,
                          category_id     UUID NOT NULL REFERENCES categories(id),
                          supplier_id     UUID NOT NULL REFERENCES suppliers(id),
                          created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_supplier ON products(supplier_id);
CREATE INDEX idx_products_active ON products(active);