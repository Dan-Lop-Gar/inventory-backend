CREATE TYPE order_status AS ENUM (
    'PENDING',
    'APPROVED',
    'RECEIVED',
    'CANCELLED',
    'FAILED'
);

CREATE TABLE purchase_orders (
                                 id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 order_number    VARCHAR(50) NOT NULL UNIQUE,
                                 status          order_status NOT NULL DEFAULT 'PENDING',
                                 supplier_id     UUID NOT NULL REFERENCES suppliers(id),
                                 total_amount    DECIMAL(14,2) NOT NULL DEFAULT 0,
                                 notes           TEXT,
                                 failure_reason  TEXT,
                                 retry_count     INTEGER NOT NULL DEFAULT 0,
                                 created_by      VARCHAR(255) NOT NULL,
                                 approved_by     VARCHAR(255),
                                 created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                                 updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                                 approved_at     TIMESTAMP,
                                 received_at     TIMESTAMP
);

CREATE TABLE purchase_order_lines (
                                      id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      order_id        UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
                                      product_id      UUID NOT NULL REFERENCES products(id),
                                      quantity        INTEGER NOT NULL CHECK (quantity > 0),
                                      unit_price      DECIMAL(12,2) NOT NULL CHECK (unit_price >= 0),
                                      total_price     DECIMAL(14,2) NOT NULL,
                                      created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_status ON purchase_orders(status);
CREATE INDEX idx_orders_supplier ON purchase_orders(supplier_id);
CREATE INDEX idx_orders_created_at ON purchase_orders(created_at);
CREATE INDEX idx_order_lines_order ON purchase_order_lines(order_id);