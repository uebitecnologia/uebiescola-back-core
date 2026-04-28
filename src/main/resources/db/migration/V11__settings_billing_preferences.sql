-- Preferencias de cobranca centralizadas na tela de Configuracoes da escola.
-- Multa, juros e pix continuam em school (Dados da Escola) — aqui adicionamos
-- apenas campos novos que ainda nao existiam, pra nao quebrar finance-service.

ALTER TABLE school_settings
    ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS discount_limit_days INTEGER,
    ADD COLUMN IF NOT EXISTS accept_pix BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS accept_boleto BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS accept_card BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS max_installments INTEGER NOT NULL DEFAULT 12,
    ADD COLUMN IF NOT EXISTS nfse_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS nfse_auto_emit BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS invoice_description VARCHAR(255);
