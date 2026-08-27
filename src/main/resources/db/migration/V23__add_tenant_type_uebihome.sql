-- Adiciona suporte a multiplos tipos de tenant (escola tradicional vs familia
-- homeschool vs escola parceira de prova homeschool). Base pro produto wedge
-- UebiHome (homeschool.uebiescola.com.br). Decisao 26/06/2026 em
-- project_uebihome_design.md.
--
-- Idempotente.

ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS tenant_type VARCHAR(30) NOT NULL DEFAULT 'SCHOOL';
-- Valores: SCHOOL (tradicional) | FAMILY (homeschool) | PARTNER_HOMESCHOOL (escola que aplica prova)

-- Familias homeschool podem se cadastrar como PARTNER_HOMESCHOOL pra aceitar
-- aplicar provas de equivalencia semestrais. Take rate UebiHome = 20%.
ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS homeschool_partner BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS homeschool_partner_radius_km INTEGER,
    ADD COLUMN IF NOT EXISTS homeschool_partner_exam_price NUMERIC(10,2);

-- Familias declaram abordagem pedagogica (filtro/match comunidade)
ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS family_pedagogy_approach VARCHAR(50);
-- Valores: ECLETICO | CHARLOTTE_MASON | MONTESSORI | CLASSICAL | RELIGIOUS | UNSCHOOLING

CREATE INDEX IF NOT EXISTS idx_schools_tenant_type ON schools(tenant_type);
CREATE INDEX IF NOT EXISTS idx_schools_homeschool_partner
    ON schools(homeschool_partner)
    WHERE homeschool_partner = TRUE;
