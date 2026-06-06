-- Tabela de leads capturados via site marketing (lead-magnets, contato).
-- Usado em /api/v1/public/leads para registrar interesse de visitantes.
--
-- Sem FK pra schools porque na maioria dos casos o lead ainda nao virou
-- escola. Quando virar, vincula via lead.email = users.email no UpgradeUseCase.

CREATE TABLE IF NOT EXISTS leads (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(160),
    resource      VARCHAR(80) NOT NULL,
    source        VARCHAR(80),
    user_agent    VARCHAR(500),
    ip_address    VARCHAR(64),
    school_id     BIGINT,
    sent_at       TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_leads_email ON leads (email);
CREATE INDEX IF NOT EXISTS idx_leads_resource ON leads (resource);
CREATE INDEX IF NOT EXISTS idx_leads_created_at ON leads (created_at DESC);
