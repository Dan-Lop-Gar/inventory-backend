CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE categories (
                            id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name        VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                            updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE suppliers (
                           id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           name         VARCHAR(200) NOT NULL,
                           email        VARCHAR(255) NOT NULL UNIQUE,
                           phone        VARCHAR(20),
                           country      VARCHAR(100),
                           address      TEXT,
                           active       BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);