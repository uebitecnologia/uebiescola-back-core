-- REVALIDACAO-ADMIN-PLATAFORMA 06/09/2026 (A-5 parcial): tela /logs promete
-- coluna "IP de origem" mas o backend nao coletava nem gravava. Adiciona a
-- coluna pra AuditAspect popular via HttpServletRequest.
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);
