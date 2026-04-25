-- Popula a tabela terms_versions com a versao inicial dos 3 termos exigidos
-- pelo fluxo LGPD do produto. Inseridos como active=true para que
-- CheckTermsStatusUseCase comece a exigir aceite de novos usuarios.
--
-- CONTEUDO E V1.0 — texto base. REVISAO JURIDICA OBRIGATORIA antes do GA.
-- Para publicar nova versao: INSERT outra linha com type igual + version novo +
-- active=true e UPDATE old SET active=false. AcceptTermsUseCase re-pede aceite.

INSERT INTO terms_versions (type, title, content, version, active, created_at, created_by) VALUES
('TERMS_OF_USE', 'Termos de Uso', $TERMS$
# Termos de Uso da Plataforma UebiEscola

**Versao 1.0 — Vigente a partir de 2026-04-25**

## 1. Aceitacao
Ao acessar e utilizar a plataforma UebiEscola, voce concorda com estes Termos de Uso e com a Politica de Privacidade. Se discordar, nao utilize a plataforma.

## 2. Definicoes
- **Plataforma**: o sistema UebiEscola, incluindo o painel administrativo, o portal do responsavel, integracoes e APIs.
- **Escola**: pessoa juridica contratante do servico, com responsabilidade pelos dados dos alunos cadastrados.
- **Usuario**: pessoa fisica que acessa a plataforma — diretor, coordenador, professor, responsavel ou aluno.

## 3. Cadastro e conta
- O cadastro requer informacoes verdadeiras (CPF, e-mail, dados da escola).
- Cada usuario e responsavel por suas credenciais. Nao compartilhe senhas.
- A escola contratante e responsavel pelos usuarios que cadastra e pelos dados dos alunos.

## 4. Uso aceitavel
Voce concorda em nao:
- Acessar dados de outras escolas sem autorizacao.
- Compartilhar credenciais com terceiros.
- Usar a plataforma para finalidade ilegal ou que viole direitos de terceiros.
- Tentar comprometer a seguranca, fazer engenharia reversa ou sobrecarregar o sistema.
- Publicar conteudo ofensivo, difamatorio, discriminatorio ou que exponha menores.

## 5. Disponibilidade
- Buscamos uptime alto, mas nao garantimos disponibilidade 100%.
- Manutencoes programadas sao avisadas com antecedencia.
- Falhas em servicos externos (Asaas, e-mail, WhatsApp) podem afetar funcionalidades.

## 6. Pagamento e cancelamento
- Os planos pagos sao cobrados conforme contrato firmado entre escola e UebiEscola.
- Cancelamento durante periodo de teste (trial) nao gera cobranca.
- Cancelamento apos contratacao segue politica de pro-rata definida no contrato.
- Inadimplencia por mais de 30 dias pode levar a suspensao do acesso, conforme regua de cobranca.

## 7. Propriedade intelectual
- A plataforma, seu codigo, marca e identidade visual sao propriedade da UebiEscola.
- Os dados inseridos pela escola permanecem propriedade da escola, que pode exporta-los a qualquer momento via funcao de exportacao.

## 8. Limitacao de responsabilidade
- A UebiEscola nao se responsabiliza por danos indiretos, lucros cessantes ou perda de dados causada por uso indevido pela escola.
- A responsabilidade da UebiEscola limita-se ao valor pago pela escola nos ultimos 12 meses.

## 9. Modificacoes nos termos
Estes termos podem ser atualizados. Mudancas relevantes serao notificadas e exigirao novo aceite.

## 10. Foro e legislacao
Estes termos sao regidos pela legislacao brasileira. Foro: comarca da sede da UebiEscola.

## 11. Contato
Duvidas: contato@uebiescola.com.br
$TERMS$, '1.0', true, CURRENT_TIMESTAMP, 'system'),

('PRIVACY_POLICY', 'Politica de Privacidade', $PRIVACY$
# Politica de Privacidade — UebiEscola

**Versao 1.0 — Vigente a partir de 2026-04-25**

## 1. Quem somos
A UebiEscola e operada por [Razao Social — preencher antes do GA], CNPJ [...]. Atuamos como **operadora** de dados em nome das escolas (controladoras) que contratam a plataforma — exceto pelos dados de cadastro proprio da escola, em que somos controladores.

## 2. Quais dados coletamos

### Da escola contratante (controlador: UebiEscola)
- Razao social, CNPJ, inscricao estadual e municipal
- Endereco e contatos
- Dados do administrador (nome, CPF, e-mail, telefone)

### Dos usuarios e alunos (controlador: a escola)
A escola insere e e responsavel por:
- Cadastro de alunos (nome, data de nascimento, CPF, foto, documentos escolares)
- Cadastro de responsaveis (nome, CPF, e-mail, telefone, endereco)
- Cadastro de professores (nome, CPF, contato, formacao)
- Notas, frequencia, observacoes pedagogicas, mensagens
- Cobrancas, pagamentos, contratos

### Coletados automaticamente
- Logs de acesso (IP, user-agent, horario)
- Logs de auditoria (acoes em entidades sensiveis)
- Cookies tecnicos para sessao e seguranca

## 3. Por que coletamos (base legal)
- **Execucao de contrato** com a escola (art. 7, V LGPD)
- **Cumprimento de obrigacao legal** (educacional, fiscal, tributaria)
- **Legitimo interesse** (seguranca, prevencao de fraude, melhoria do produto)
- **Consentimento** quando aplicavel (notificacoes opcionais, marketing)

## 4. Com quem compartilhamos
- **Operadores** (provedores de servico): Google Cloud (hospedagem), Asaas (pagamentos), Zoho (e-mail). Vinculados por contrato com clausulas de protecao de dados.
- **Autoridades** quando exigido por lei ou ordem judicial.
- **Nao vendemos** dados a terceiros para fins comerciais.

## 5. Quanto tempo guardamos
- Dados ativos: enquanto a escola for cliente.
- Apos cancelamento: dados ficam disponiveis para exportacao por 30 dias, depois sao apagados (soft-delete + purga em 30 dias adicionais por exigencia legal de retencao educacional).
- Backups: ate 90 dias.
- Logs de auditoria: 5 anos (exigencia legal e tecnica).

## 6. Seguranca
- Criptografia em transito (TLS 1.2+) e em repouso.
- Acesso por roles, autenticacao por JWT, expiracao de sessao.
- Auditoria de acoes sensiveis.
- Segregacao multi-tenant: escolas nao acessam dados umas das outras.

## 7. Seus direitos LGPD
Voce pode, a qualquer momento, solicitar via portal ou e-mail dpo@uebiescola.com.br:
- **Acesso** aos dados que mantemos sobre voce
- **Correcao** de dados incorretos
- **Exclusao** dos seus dados (respeitada a obrigacao legal de retencao)
- **Portabilidade** (exportar em formato estruturado)
- **Anonimizacao**
- **Informacao** sobre com quem compartilhamos
- **Revogacao** de consentimento

Solicitacoes sao processadas em ate 15 dias.

## 8. Dados de menores
Alunos menores de idade tem dados tratados sob responsabilidade dos pais/responsaveis legais e da escola. So coletamos dados estritamente necessarios a finalidade educacional. Nao usamos dados de menores para publicidade.

## 9. Cookies
Usamos cookies tecnicos para sessao e seguranca. Nao usamos cookies de tracking de terceiros sem consentimento.

## 10. Encarregado (DPO)
Contato: dpo@uebiescola.com.br

## 11. Alteracoes
Esta politica pode ser atualizada. Mudancas relevantes serao notificadas com 30 dias de antecedencia.
$PRIVACY$, '1.0', true, CURRENT_TIMESTAMP, 'system'),

('DATA_PROCESSING', 'Termo de Tratamento de Dados (DPA)', $DPA$
# Termo de Tratamento de Dados — UebiEscola Operador / Escola Controlador

**Versao 1.0 — Vigente a partir de 2026-04-25**

Este termo regula o tratamento de dados pessoais entre a Escola (controladora) e a UebiEscola (operadora), nos termos da Lei 13.709/2018 (LGPD).

## 1. Objeto
A UebiEscola tratara dados pessoais inseridos pela Escola na plataforma, exclusivamente para execucao do contrato de prestacao de servicos.

## 2. Naturezas dos dados tratados
- Dados cadastrais de alunos, responsaveis, professores e funcionarios
- Dados pedagogicos: notas, frequencia, observacoes
- Dados financeiros: cobrancas, pagamentos, contratos
- Dados de comunicacao: mensagens, comunicados, anexos
- Dados sensiveis (quando inseridos pela escola): laudos medicos, dados religiosos opcionais, raca/etnia para fins de censo escolar

## 3. Obrigacoes da Escola (controladora)
- Garantir base legal para o tratamento (geralmente contrato escolar + obrigacao legal educacional).
- Informar titulares sobre o tratamento.
- Cadastrar apenas dados necessarios a finalidade educacional, financeira ou de comunicacao da escola.
- Atender solicitacoes dos titulares (alunos, responsaveis) — a UebiEscola fornece os meios tecnicos via portal.
- Manter atualizado o cadastro do administrador da conta.

## 4. Obrigacoes da UebiEscola (operadora)
- Tratar dados estritamente conforme instrucao da Escola e do contrato.
- Manter medidas tecnicas e organizacionais de seguranca: criptografia, controle de acesso, auditoria, backup, segregacao multi-tenant.
- Notificar incidentes de seguranca em ate 48h apos detecao.
- Disponibilizar mecanismos de exportacao, exclusao, correcao e anonimizacao dos dados.
- Nao subcontratar operadores sem comunicar a Escola.
- Apos termino do contrato: disponibilizar exportacao por 30 dias, depois apagar — exceto pelo prazo legal de retencao educacional.

## 5. Subcontratados (suboperadores)
A UebiEscola declara utilizar:
- **Google Cloud Platform** (hospedagem em us-central1)
- **Asaas** (gateway de pagamento)
- **Zoho Mail** (envio de e-mails transacionais)

A Escola declara ciencia e concorda com estes suboperadores. Mudancas serao comunicadas com 30 dias.

## 6. Transferencia internacional
Os servidores estao localizados nos EUA (Google Cloud us-central1). A transferencia ocorre conforme art. 33 da LGPD — pais com nivel adequado de protecao via clausulas contratuais padroes.

## 7. Incidentes de seguranca
A UebiEscola notificara a Escola por e-mail e via portal em ate 48h apos detecao de incidente que possa afetar dados pessoais. A Escola e responsavel por comunicar ANPD e titulares quando exigido.

## 8. Auditoria
A Escola pode solicitar relatorio anual de medidas de seguranca aplicadas. Auditoria in-loco ou por terceiros pode ser combinada com 30 dias de antecedencia, com custos por conta da Escola.

## 9. Encerramento
- A Escola pode solicitar exclusao integral dos dados a qualquer momento, respeitada retencao legal (5 anos para auditoria fiscal, prazos educacionais aplicaveis).
- Apos exclusao, fornecemos relatorio confirmando.

## 10. Foro
Comarca da sede da UebiEscola, conforme Termos de Uso.
$DPA$, '1.0', true, CURRENT_TIMESTAMP, 'system');
