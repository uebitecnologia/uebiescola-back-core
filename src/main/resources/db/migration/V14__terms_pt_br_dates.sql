-- Corrige formato de data nos termos para padrao PT-BR (DD/MM/YYYY).
-- V13 inseriu o conteudo com data ISO "2026-04-25" no cabecalho. O usuario
-- final brasileiro espera "25/04/2026". UPDATE em vez de novo INSERT porque
-- nao queremos forcar re-aceite de quem ja aceitou — a mudanca e cosmetica
-- no texto de cabecalho, nao no conteudo juridico.

UPDATE terms_versions
SET content = REPLACE(content, 'Vigente a partir de 2026-04-25', 'Vigente a partir de 25/04/2026')
WHERE active = true
  AND content LIKE '%Vigente a partir de 2026-04-25%';
