-- Programa de indicacao R$200/contrato (decisao 04/06/2026, feature #66).
--
-- Cada escola ganha um codigo de indicacao curto (8 chars) que pode ser
-- compartilhado via URL. Quando uma nova escola se cadastra usando esse
-- codigo, criamos um registro em `referrals` com status=PENDING.
--
-- O credito de R$200 e dado depois do primeiro pagamento confirmado
-- da escola indicada (hook fica fora dessa migration; sera implementado
-- num consumer de evento invoice.paid).

-- 1) Coluna referral_code em schools (nullable + unique)
ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS referral_code VARCHAR(8);

CREATE UNIQUE INDEX IF NOT EXISTS uk_schools_referral_code
    ON schools (referral_code)
    WHERE referral_code IS NOT NULL;

-- 2) Backfill: gera codigo pras escolas existentes
-- substr(md5(...), 1, 8) e suficiente; risco de colisao em <1000 escolas e
-- desprezivel. Caso colida, o app gera outro no save da entidade.
UPDATE schools
SET referral_code = UPPER(SUBSTR(MD5(RANDOM()::TEXT || id::TEXT), 1, 8))
WHERE referral_code IS NULL;

-- 3) Tabela de indicacoes
CREATE TABLE IF NOT EXISTS referrals (
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID NOT NULL UNIQUE,
    referrer_school_id   BIGINT NOT NULL REFERENCES schools(id),
    referred_school_id   BIGINT NOT NULL UNIQUE REFERENCES schools(id),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    credit_value         NUMERIC(10,2) NOT NULL DEFAULT 200.00,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    credited_at          TIMESTAMP,
    cancelled_at         TIMESTAMP,
    cancel_reason        VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_referrals_referrer
    ON referrals (referrer_school_id);

CREATE INDEX IF NOT EXISTS idx_referrals_status
    ON referrals (status);
