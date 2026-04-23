-- Verificacao de email pos-cadastro (self-service). Cada usuario recebe um
-- codigo de 6 digitos por email e precisa confirmar antes de logar.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified_at              TIMESTAMP,
    ADD COLUMN IF NOT EXISTS email_verification_code        VARCHAR(6),
    ADD COLUMN IF NOT EXISTS email_verification_expires_at  TIMESTAMP;

-- Usuarios existentes ja sao considerados verificados (evita bloquear
-- logins ja ativos apos o deploy).
UPDATE users SET email_verified_at = NOW() WHERE email_verified_at IS NULL;
