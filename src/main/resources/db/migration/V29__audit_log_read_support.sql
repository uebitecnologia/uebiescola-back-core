-- A-5 AUDITORIAADMINPLATAFORMA 03/09/2026: log de leitura pra CEO.
--
-- Chrome mediu 1.065 registros no /v1/audit, todos comecando com "Criou",
-- "Atualizou" ou "Editou" — nenhum "Consultou". Equipe UebiEscola abrindo
-- dados de uma escola-cliente nao ficava registro. LGPD art. 37 exige o
-- registro das operacoes de tratamento; art. 46, medidas de seguranca.
--
-- Duas mudancas de schema:
--   1) school_id NULLABLE — CEO fazendo leitura cross-tenant (ex.: listar
--      escolas) nao tem target unico; permitir null e menos ruidoso do que
--      criar entradas duplicadas por schoolId.
--   2) Indice em created_at pra suportar politica de retencao futura
--      (audit doc pediu prazo declarado no DPA — proxima rodada).
ALTER TABLE audit_logs ALTER COLUMN school_id DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at);
