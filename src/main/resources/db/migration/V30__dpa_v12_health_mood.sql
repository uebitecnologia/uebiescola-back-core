-- A-4 AUDITORIAADMINPLATAFORMA 03/09/2026: DPA v1.1 nao declarava dados
-- de saude (tipo sanguineo, medicacao continua, laudos), nem dados de
-- bem-estar emocional (Mapa de Humor). Feature vendida no plano Professional
-- que coleta estado emocional de menor diariamente ficava sem base no
-- instrumento que rege o tratamento — gemeo do L-4 no lado da escola.
--
-- Publica v1.2 desativando a v1.1 (preserva trilha).

UPDATE terms_versions SET active = false
WHERE type = 'DATA_PROCESSING' AND version = '1.1' AND active = true;

INSERT INTO terms_versions (type, title, content, version, active, created_at, created_by, uuid) VALUES
('DATA_PROCESSING', 'Termo de Tratamento de Dados (DPA)', $DPA$
# Termo de Tratamento de Dados — UebiEscola Operador / Escola Controlador

**Versão 1.2 — Vigente a partir de 04/09/2026**

Este termo regula o tratamento de dados pessoais entre a Escola (controladora) e a UebiEscola (operadora), nos termos da Lei 13.709/2018 (LGPD).

## 1. Objeto
A UebiEscola tratará dados pessoais inseridos pela Escola na plataforma, exclusivamente para execução do contrato de prestação de serviços.

## 2. Naturezas dos dados tratados
- Dados cadastrais de alunos, responsáveis, professores e funcionários (nome, CPF, RG, endereço, contatos)
- Dados pedagógicos: notas, frequência, observações
- Dados financeiros: cobranças, pagamentos, contratos
- Dados de comunicação: mensagens, comunicados, anexos
- **Dados de saúde (sensíveis, art. 11 LGPD)**, quando inseridos pela Escola: tipo sanguíneo, alergias, medicação de uso contínuo, laudos médicos (deficiência, AEE)
- **Dados de bem-estar/estado emocional (sensíveis)**, quando ativada a feature Mapa de Humor no plano contratado: registro diário do humor autodeclarado pelo aluno, com finalidade pedagógica e de acompanhamento socioemocional
- **Dados religiosos** (opcionais), quando informados voluntariamente
- **Raça/etnia**, exclusivamente para fins do Censo Escolar INEP
- **Dados de menores** (art. 14 §1º): todos os dados acima referentes a alunos menores são tratados sob responsabilidade da Escola e dos pais/responsáveis legais, mediante consentimento registrado no portal (5 finalidades: dados cadastrais, saúde, comunicação por WhatsApp, imagem/voz, bem-estar emocional)

## 3. Obrigações da Escola (controladora)
- Garantir base legal para o tratamento (geralmente contrato escolar + obrigação legal educacional).
- Informar titulares sobre o tratamento.
- Cadastrar apenas dados necessários à finalidade educacional, financeira ou de comunicação da escola.
- Atender solicitações dos titulares (alunos, responsáveis) — a UebiEscola fornece os meios técnicos via portal.
- Manter atualizado o cadastro do administrador da conta.
- Habilitar a coleta de dados sensíveis (saúde, humor) apenas com base legal específica (consentimento dos responsáveis + finalidade declarada).

## 4. Obrigações da UebiEscola (operadora)
- Tratar dados estritamente conforme instrução da Escola e do contrato.
- Manter medidas técnicas e organizacionais de segurança: criptografia, controle de acesso, auditoria, backup, segregação multi-tenant.
- Notificar incidentes de segurança em até 48h após detecção.
- Disponibilizar mecanismos de exportação, exclusão, correção e anonimização dos dados.
- Não subcontratar operadores sem comunicar a Escola.
- Após término do contrato: disponibilizar exportação por 30 dias, depois apagar — exceto pelo prazo legal de retenção educacional.

## 5. Subcontratados (suboperadores)
A UebiEscola declara utilizar:
- **Amazon Web Services** (hospedagem em sa-east-1)
- **Asaas** (gateway de pagamento)
- **Zoho Mail** (envio de e-mails transacionais)

A Escola declara ciência e concorda com estes suboperadores. Mudanças serão comunicadas com 30 dias.

## 6. Transferência internacional
Os servidores estão localizados no Brasil (AWS sa-east-1). Não há transferência internacional na operação regular. Caso venha a ocorrer, será conforme art. 33 da LGPD — país com nível adequado de proteção ou cláusulas contratuais padrões.

## 7. Incidentes de segurança
A UebiEscola notificará a Escola por e-mail e via portal em até 48h após detecção de incidente que possa afetar dados pessoais. A Escola é responsável por comunicar ANPD e titulares quando exigido.

## 8. Auditoria
A Escola pode solicitar relatório anual de medidas de segurança aplicadas. Auditoria in-loco ou por terceiros pode ser combinada com 30 dias de antecedência, com custos por conta da Escola.

## 9. Retenção específica
- **Dados de saúde**: mantidos enquanto o aluno estiver matriculado + prazo legal educacional. Excluídos por solicitação de titular ou responsável.
- **Mapa de Humor**: registros individuais mantidos por 12 meses; agregados anônimos podem ser retidos indefinidamente para fins pedagógicos.
- **Logs de auditoria**: 5 anos (exigência legal e técnica).

## 10. Encerramento
- A Escola pode solicitar exclusão integral dos dados a qualquer momento, respeitada retenção legal (5 anos para auditoria fiscal, prazos educacionais aplicáveis).
- Após exclusão, fornecemos relatório confirmando.

## 11. Foro
Comarca da sede da UebiEscola, conforme Termos de Uso.
$DPA$, '1.2', true, NOW(), 'system', gen_random_uuid());
