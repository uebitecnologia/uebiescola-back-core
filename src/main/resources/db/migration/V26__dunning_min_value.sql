-- R-2 rodada 10 (31/08 pente-fino): valor minimo pra a regua disparar.
-- Faturas abaixo desse valor nao geram cobranca automatica.

ALTER TABLE school_settings
ADD COLUMN IF NOT EXISTS dunning_min_value NUMERIC(10,2) NOT NULL DEFAULT 10.00;
