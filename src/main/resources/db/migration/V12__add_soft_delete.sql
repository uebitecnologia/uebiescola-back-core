-- Soft delete em entidades-raiz com PII/historico legal.
-- Hibernate 6.5 @SoftDelete(strategy=TIMESTAMP, columnName="deleted_at"):
--   deleted_at IS NULL  -> linha ativa
--   deleted_at NOT NULL -> linha logicamente removida (timestamp do delete)

ALTER TABLE schools             ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
ALTER TABLE users               ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
ALTER TABLE school_contracts    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
ALTER TABLE terms_acceptances   ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- Indices parciais (so linhas ativas) pra manter listagens rapidas.
CREATE INDEX IF NOT EXISTS idx_schools_active           ON schools(id)            WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_active             ON users(id)              WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_school_contracts_active  ON school_contracts(id)   WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_terms_acceptances_active ON terms_acceptances(id)  WHERE deleted_at IS NULL;
