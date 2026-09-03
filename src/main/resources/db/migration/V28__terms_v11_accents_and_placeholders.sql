-- Auditoria admin plataforma 03/09/2026 (Chrome): A-1 + A-2.
--
-- A-1: Politica de Privacidade v1.0 tem placeholders literais "[Razao Social —
-- preencher antes do GA]" e CNPJ vazio na primeira frase da secao que
-- identifica o controlador. Corrige preservando trilha via versionamento —
-- desativa v1.0 e publica v1.1 com Uebi Tecnologia Ltda + CNPJ.
--
-- A-2: os 3 documentos legais (TERMS_OF_USE, PRIVACY_POLICY, DATA_PROCESSING)
-- somaram 9.170 chars sem UM UNICO ACENTO. Reescrita com acentuacao correta
-- pra nao ler como texto gerado por maquina em analise juridica.
--
-- Tambem: A-3 corrigido — vigencia agora bate com data de publicacao (03/09/2026)
-- em vez de 25/04/2026 que era anterior a criacao dos registros.
--
-- NAO ALTERA schema. So INSERT + UPDATE active flag.

-- 1. Desativa v1.0 dos 3 documentos (mantem historico)
UPDATE terms_versions SET active = false
WHERE active = true AND type IN ('TERMS_OF_USE', 'PRIVACY_POLICY', 'DATA_PROCESSING')
  AND version = '1.0';

-- 2. Publica v1.1 acentuada com placeholders preenchidos

INSERT INTO terms_versions (type, title, content, version, active, created_at, created_by, uuid) VALUES
('TERMS_OF_USE', 'Termos de Uso', $TERMS$
# Termos de Uso da Plataforma UebiEscola

**Versão 1.1 — Vigente a partir de 03/09/2026**

## 1. Aceitação
Ao acessar e utilizar a plataforma UebiEscola, você concorda com estes Termos de Uso e com a Política de Privacidade. Se discordar, não utilize a plataforma.

## 2. Definições
- **Plataforma**: o sistema UebiEscola, incluindo o painel administrativo, o portal do responsável, integrações e APIs.
- **Escola**: pessoa jurídica contratante do serviço, com responsabilidade pelos dados dos alunos cadastrados.
- **Usuário**: pessoa física que acessa a plataforma — diretor, coordenador, professor, responsável ou aluno.

## 3. Cadastro e conta
- O cadastro requer informações verdadeiras (CPF, e-mail, dados da escola).
- Cada usuário é responsável por suas credenciais. Não compartilhe senhas.
- A escola contratante é responsável pelos usuários que cadastra e pelos dados dos alunos.

## 4. Uso aceitável
Você concorda em não:
- Acessar dados de outras escolas sem autorização.
- Compartilhar credenciais com terceiros.
- Usar a plataforma para finalidade ilegal ou que viole direitos de terceiros.
- Tentar comprometer a segurança, fazer engenharia reversa ou sobrecarregar o sistema.
- Publicar conteúdo ofensivo, difamatório, discriminatório ou que exponha menores.

## 5. Disponibilidade
- Buscamos uptime alto, mas não garantimos disponibilidade 100%.
- Manutenções programadas são avisadas com antecedência.
- Falhas em serviços externos (Asaas, e-mail, WhatsApp) podem afetar funcionalidades.

## 6. Pagamento e cancelamento
- Os planos pagos são cobrados conforme contrato firmado entre escola e UebiEscola.
- Cancelamento durante período de teste (trial) não gera cobrança.
- Cancelamento após contratação segue política de pró-rata definida no contrato.
- Inadimplência por mais de 30 dias pode levar à suspensão do acesso, conforme régua de cobrança.

## 7. Propriedade intelectual
- A plataforma, seu código, marca e identidade visual são propriedade da UebiEscola.
- Os dados inseridos pela escola permanecem propriedade da escola, que pode exportá-los a qualquer momento via função de exportação.

## 8. Limitação de responsabilidade
- A UebiEscola não se responsabiliza por danos indiretos, lucros cessantes ou perda de dados causada por uso indevido pela escola.
- A responsabilidade da UebiEscola limita-se ao valor pago pela escola nos últimos 12 meses.

## 9. Modificações nos termos
Estes termos podem ser atualizados. Mudanças relevantes serão notificadas e exigirão novo aceite.

## 10. Foro e legislação
Estes termos são regidos pela legislação brasileira. Foro: comarca da sede da UebiEscola.

## 11. Contato
Dúvidas: contato@uebiescola.com.br
$TERMS$, '1.1', true, NOW(), 'system', gen_random_uuid()),

('PRIVACY_POLICY', 'Política de Privacidade', $PRIVACY$
# Política de Privacidade — UebiEscola

**Versão 1.1 — Vigente a partir de 03/09/2026**

## 1. Quem somos
A UebiEscola é operada por **Uebi Tecnologia Ltda**, CNPJ **62.488.065/0001-85**. Atuamos como **operadora** de dados em nome das escolas (controladoras) que contratam a plataforma — exceto pelos dados de cadastro próprio da escola, em que somos controladores.

## 2. Quais dados coletamos

### Da escola contratante (controlador: UebiEscola)
- Razão social, CNPJ, inscrição estadual e municipal
- Endereço e contatos
- Dados do administrador (nome, CPF, e-mail, telefone)

### Dos usuários e alunos (controlador: a escola)
A escola insere e é responsável por:
- Cadastro de alunos (nome, data de nascimento, CPF, foto, documentos escolares)
- Cadastro de responsáveis (nome, CPF, e-mail, telefone, endereço)
- Cadastro de professores (nome, CPF, contato, formação)
- Notas, frequência, observações pedagógicas, mensagens
- Cobranças, pagamentos, contratos

### Coletados automaticamente
- Logs de acesso (IP, user-agent, horário)
- Logs de auditoria (ações em entidades sensíveis)
- Cookies técnicos para sessão e segurança

## 3. Por que coletamos (base legal)
- **Execução de contrato** com a escola (art. 7º, V LGPD)
- **Cumprimento de obrigação legal** (educacional, fiscal, tributária)
- **Legítimo interesse** (segurança, prevenção de fraude, melhoria do produto)
- **Consentimento** quando aplicável (notificações opcionais, marketing)

## 4. Com quem compartilhamos
- **Operadores** (provedores de serviço): Amazon Web Services (hospedagem em sa-east-1), Asaas (pagamentos), Zoho (e-mail). Vinculados por contrato com cláusulas de proteção de dados.
- **Autoridades** quando exigido por lei ou ordem judicial.
- **Não vendemos** dados a terceiros para fins comerciais.

## 5. Quanto tempo guardamos
- Dados ativos: enquanto a escola for cliente.
- Após cancelamento: dados ficam disponíveis para exportação por 30 dias, depois são apagados (soft-delete + purga em 30 dias adicionais por exigência legal de retenção educacional).
- Backups: até 90 dias.
- Logs de auditoria: 5 anos (exigência legal e técnica).

## 6. Segurança
- Criptografia em trânsito (TLS 1.2+) e em repouso.
- Acesso por roles, autenticação por JWT, expiração de sessão.
- Auditoria de ações sensíveis.
- Segregação multi-tenant: escolas não acessam dados umas das outras.

## 7. Seus direitos LGPD
Você pode, a qualquer momento, solicitar via portal ou e-mail dpo@uebiescola.com.br:
- **Acesso** aos dados que mantemos sobre você
- **Correção** de dados incorretos
- **Exclusão** dos seus dados (respeitada a obrigação legal de retenção)
- **Portabilidade** (exportar em formato estruturado)
- **Anonimização**
- **Informação** sobre com quem compartilhamos
- **Revogação** de consentimento

Solicitações são processadas em até 15 dias.

## 8. Dados de menores
Alunos menores de idade têm dados tratados sob responsabilidade dos pais/responsáveis legais e da escola. Só coletamos dados estritamente necessários à finalidade educacional. Não usamos dados de menores para publicidade.

## 9. Cookies
Usamos cookies técnicos para sessão e segurança. Não usamos cookies de tracking de terceiros sem consentimento.

## 10. Encarregado (DPO)
Contato: dpo@uebiescola.com.br

## 11. Alterações
Esta política pode ser atualizada. Mudanças relevantes serão notificadas com 30 dias de antecedência.
$PRIVACY$, '1.1', true, NOW(), 'system', gen_random_uuid()),

('DATA_PROCESSING', 'Termo de Tratamento de Dados (DPA)', $DPA$
# Termo de Tratamento de Dados — UebiEscola Operador / Escola Controlador

**Versão 1.1 — Vigente a partir de 03/09/2026**

Este termo regula o tratamento de dados pessoais entre a Escola (controladora) e a UebiEscola (operadora), nos termos da Lei 13.709/2018 (LGPD).

## 1. Objeto
A UebiEscola tratará dados pessoais inseridos pela Escola na plataforma, exclusivamente para execução do contrato de prestação de serviços.

## 2. Naturezas dos dados tratados
- Dados cadastrais de alunos, responsáveis, professores e funcionários
- Dados pedagógicos: notas, frequência, observações
- Dados financeiros: cobranças, pagamentos, contratos
- Dados de comunicação: mensagens, comunicados, anexos
- Dados sensíveis (quando inseridos pela escola): laudos médicos, dados religiosos opcionais, raça/etnia para fins de censo escolar

## 3. Obrigações da Escola (controladora)
- Garantir base legal para o tratamento (geralmente contrato escolar + obrigação legal educacional).
- Informar titulares sobre o tratamento.
- Cadastrar apenas dados necessários à finalidade educacional, financeira ou de comunicação da escola.
- Atender solicitações dos titulares (alunos, responsáveis) — a UebiEscola fornece os meios técnicos via portal.
- Manter atualizado o cadastro do administrador da conta.

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

## 9. Encerramento
- A Escola pode solicitar exclusão integral dos dados a qualquer momento, respeitada retenção legal (5 anos para auditoria fiscal, prazos educacionais aplicáveis).
- Após exclusão, fornecemos relatório confirmando.

## 10. Foro
Comarca da sede da UebiEscola, conforme Termos de Uso.
$DPA$, '1.1', true, NOW(), 'system', gen_random_uuid());
