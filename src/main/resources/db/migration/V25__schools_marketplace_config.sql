-- Configuracao da Loja da escola (marketplace por escola, portal do responsavel).
--
-- Modelo novo: a Loja NAO eh mais uma plataforma multi-editora — cada escola
-- vende seus proprios materiais (uniforme, apostila, livros, kit) direto pro
-- responsavel. UebiEscola retem uma comissao (default 10%) via split Asaas na
-- subconta da escola.
--
-- Campos:
--   marketplace_enabled: toggle ativa Loja pra essa escola (default false)
--   marketplace_commission_percent: % que UebiEscola retem sobre GMV (default 10)
--   marketplace_commission_cap: teto absoluto por pedido (opcional — evita
--     comissao gigante em pedido de kit escolar completo)
--   marketplace_settings: JSONB configuravel pela escola:
--     {
--       "storeName": "Loja do Colegio X",
--       "storeBanner": "Bem-vindo ao nosso material didatico",
--       "pickupLocation": "Secretaria (Bloco A, 8h-17h)",
--       "pickupPromise": "Separamos em ate 2 dias uteis apos pagamento",
--       "pickupExpirationDays": 15,
--       "paymentMethods": ["PIX", "BOLETO", "CREDIT_CARD"],
--       "issueReceipt": true,
--       "issueNfse": false,
--       "notifyOnNewOrder": {"email": true, "whatsapp": true},
--       "guardianCanCancelBeforePayment": true,
--       "hideOutOfStock": true,
--       "stockReserveHours": 24
--     }

ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS marketplace_enabled           BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS marketplace_commission_percent NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    ADD COLUMN IF NOT EXISTS marketplace_commission_cap    NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS marketplace_settings          JSONB       NOT NULL DEFAULT '{}'::jsonb;

-- Constraint: comissao entre 0 e 100
ALTER TABLE schools
    ADD CONSTRAINT schools_marketplace_commission_range
    CHECK (marketplace_commission_percent >= 0 AND marketplace_commission_percent <= 100);
