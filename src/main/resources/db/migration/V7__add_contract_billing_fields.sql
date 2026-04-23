-- Adiciona ciclo e forma de cobranca no contrato da escola.
-- Valores refletem a subscription correspondente no plans-service / Asaas.
ALTER TABLE school_contracts
    ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(16),
    ADD COLUMN IF NOT EXISTS billing_type  VARCHAR(16);

-- Backfill: escolas antigas assumimos mensal com forma indefinida.
UPDATE school_contracts SET billing_cycle = 'MONTHLY' WHERE billing_cycle IS NULL;
UPDATE school_contracts SET billing_type  = 'UNDEFINED' WHERE billing_type IS NULL;
