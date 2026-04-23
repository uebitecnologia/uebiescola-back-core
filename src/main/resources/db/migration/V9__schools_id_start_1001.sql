-- IDs de escolas comecam em 1001 pra evitar confusao com IDs internos
-- (usuarios, planos, etc) e pra facilitar identificacao em logs/suporte.
-- Novas escolas pegam o proximo valor da sequence. Escolas existentes
-- mantem seus IDs atuais — so altera o valor seguinte.
DO $$
DECLARE
    cur_max BIGINT;
    next_val BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 1000) INTO cur_max FROM schools;
    IF cur_max < 1000 THEN
        next_val := 1001;
    ELSE
        next_val := cur_max + 1;
    END IF;
    -- Tenta localizar e ajustar a sequence da PK (nome pode variar).
    PERFORM setval(pg_get_serial_sequence('schools', 'id'), next_val, false);
END $$;
