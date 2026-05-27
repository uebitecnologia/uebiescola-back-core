-- Plano Solo (V25 plans-service) abre o produto pra profissional liberal de
-- educacao. Profissional pode ser PF (so CPF) ou PJ (CNPJ + razao social).
-- Hoje schools.cnpj e schools.legal_name sao NOT NULL — bloqueia PF.
--
-- Esta migration:
--  1. Torna cnpj e legal_name nullable
--  2. Adiciona coluna cpf (unique, nullable) e birth_date (data de nascimento,
--     exigida pelo Asaas pra subconta PF)
--  3. Adiciona check constraint: exatamente um de (cnpj, cpf) preenchido
--  4. Backfill: schools existentes ja tem cnpj — nada a fazer

ALTER TABLE schools
    ALTER COLUMN cnpj DROP NOT NULL,
    ALTER COLUMN legal_name DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(14) UNIQUE,
    ADD COLUMN IF NOT EXISTS birth_date DATE;

-- Constraint: pelo menos um identificador fiscal preenchido. NAO usa XOR
-- estrito porque em tese poderia ter ambos (PJ que tambem registra CPF do
-- titular), embora hoje nao seja o caso — fica permissivo.
ALTER TABLE schools
    ADD CONSTRAINT schools_cpf_or_cnpj_required
    CHECK (cnpj IS NOT NULL OR cpf IS NOT NULL);
