CREATE TYPE movement_type AS ENUM (
    'PURCHASE_IN',
    'SALE_OUT',
    'ADJUSTMENT_IN',
    'ADJUSTMENT_OUT',
    'RETURN_IN'
);

CREATE TABLE stock_movements (
                                 id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 product_id      UUID NOT NULL REFERENCES products(id),
                                 movement_type   movement_type NOT NULL,
                                 quantity        INTEGER NOT NULL,
                                 stock_before    INTEGER NOT NULL,
                                 stock_after     INTEGER NOT NULL,
                                 reference_id    UUID,
                                 reference_type  VARCHAR(50),
                                 notes           TEXT,
                                 created_by      VARCHAR(255) NOT NULL,
                                 created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_movements_product ON stock_movements(product_id);
CREATE INDEX idx_movements_type ON stock_movements(movement_type);
CREATE INDEX idx_movements_created_at ON stock_movements(created_at);