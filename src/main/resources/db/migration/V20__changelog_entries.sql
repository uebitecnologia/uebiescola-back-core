-- Changelog publico: CEO publica novidades, plataforma exibe no site marketing
-- e (futuro) no admin escola. Endpoint /api/v1/public/changelog nao requer auth.

CREATE TABLE IF NOT EXISTS changelog_entries (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    title         VARCHAR(200) NOT NULL,
    summary       TEXT NOT NULL,
    content       TEXT NOT NULL,
    category      VARCHAR(24) NOT NULL CHECK (category IN ('FEATURE','IMPROVEMENT','FIX','INFRA')),
    published     BOOLEAN NOT NULL DEFAULT FALSE,
    published_at  TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index pra listar publicados em ordem desc (endpoint publico mais quente).
CREATE INDEX IF NOT EXISTS idx_changelog_published
    ON changelog_entries(published_at DESC)
    WHERE published = TRUE;
