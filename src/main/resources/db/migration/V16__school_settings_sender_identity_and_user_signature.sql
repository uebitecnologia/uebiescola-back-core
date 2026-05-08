-- Identidade da escola nas comunicações: nome exibido como remetente + email Reply-To.
-- O From real continua sendo noreply@uebiescola.com.br pra passar SPF/DKIM;
-- esses campos só personalizam o "nome do remetente" e o "responder para".
ALTER TABLE school_settings ADD COLUMN IF NOT EXISTS sender_name VARCHAR(120);
ALTER TABLE school_settings ADD COLUMN IF NOT EXISTS sender_email VARCHAR(180);

-- Perfil do usuário: cargo + assinatura usada em comunicados/templates.
ALTER TABLE users ADD COLUMN IF NOT EXISTS job_title VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS signature TEXT;
