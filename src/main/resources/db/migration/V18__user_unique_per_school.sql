-- Permite que o mesmo CPF/email exista em escolas diferentes da plataforma.
-- Antes: unique global em users(cpf) e users(email) — bloqueava transferência de
-- aluno/responsável entre escolas. Agora: unique parcial por (cpf, school_id) e
-- (email, school_id), apenas pra registros ATIVOS (deleted_at IS NULL).
--
-- Justificativa: aluno que se transfere → o cadastro antigo permanece soft-deletado
-- (preservando histórico de notas/faturas na escola anterior) e a nova escola pode
-- criar um cadastro independente. Validação cross-school pra ROLE_CEO segue no código
-- (CEO deve ser único globalmente — não tem schoolId fixo).

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_cpf_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

CREATE UNIQUE INDEX IF NOT EXISTS users_cpf_school_active
    ON users(cpf, school_id)
    WHERE deleted_at IS NULL AND cpf IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS users_email_school_active
    ON users(email, school_id)
    WHERE deleted_at IS NULL;
