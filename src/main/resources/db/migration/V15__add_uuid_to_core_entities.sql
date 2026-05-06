-- Onda UUID core: adiciona uuid em schools, access_levels, terms_versions.
-- Schools ja tem coluna external_id (UUID); mantemos para nao quebrar consumidores
-- existentes e adicionamos uuid canonico (nome alinhado com os demais services).
-- access_levels e terms_versions ganham uuid pela primeira vez.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ===================== schools =====================
ALTER TABLE schools ADD COLUMN uuid UUID;
UPDATE schools SET uuid = COALESCE(external_id, gen_random_uuid()) WHERE uuid IS NULL;
ALTER TABLE schools ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE schools ALTER COLUMN uuid SET DEFAULT gen_random_uuid();
ALTER TABLE schools ADD CONSTRAINT schools_uuid_unique UNIQUE (uuid);
CREATE INDEX idx_schools_uuid ON schools(uuid);

-- ===================== access_levels =====================
ALTER TABLE access_levels ADD COLUMN uuid UUID;
UPDATE access_levels SET uuid = gen_random_uuid() WHERE uuid IS NULL;
ALTER TABLE access_levels ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE access_levels ALTER COLUMN uuid SET DEFAULT gen_random_uuid();
ALTER TABLE access_levels ADD CONSTRAINT access_levels_uuid_unique UNIQUE (uuid);
CREATE INDEX idx_access_levels_uuid ON access_levels(uuid);

-- ===================== terms_versions =====================
ALTER TABLE terms_versions ADD COLUMN uuid UUID;
UPDATE terms_versions SET uuid = gen_random_uuid() WHERE uuid IS NULL;
ALTER TABLE terms_versions ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE terms_versions ALTER COLUMN uuid SET DEFAULT gen_random_uuid();
ALTER TABLE terms_versions ADD CONSTRAINT terms_versions_uuid_unique UNIQUE (uuid);
CREATE INDEX idx_terms_versions_uuid ON terms_versions(uuid);
