-- L-4 rodada 17 (BACKLOGFINAL Chrome): encarregado de dados (DPO) da
-- escola. LGPD art. 41 exige indicacao formal. Exibido no portal do
-- responsavel como contato pra exercicio de direitos.

ALTER TABLE school_settings
ADD COLUMN IF NOT EXISTS dpo_name VARCHAR(200);

ALTER TABLE school_settings
ADD COLUMN IF NOT EXISTS dpo_email VARCHAR(200);
