-- A-7 AUDITORIAADMINPLATAFORMA 03/09/2026: rastreio de ultimo login por usuario.
-- Auditor apontou que /users/ceo-team nao trazia lastLogin, entao nao havia
-- como saber quem da equipe entrou onde. Coluna usada tambem pra sinalizar
-- contas sem acesso recente na tela "Equipe & Permissoes".
--
-- Idempotente: iam e core compartilham a tabela `users`; usar IF NOT EXISTS
-- evita conflito caso o outro service adicione a coluna primeiro.

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;
