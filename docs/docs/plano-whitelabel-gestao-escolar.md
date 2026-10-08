# Plano — whitelabel de gestão escolar

> **Status:** proposta · 2026-10-07
> **Base:** leitura deste repositório, do vault Obsidian (`sing-microservicos/`, `sing/`) e pesquisa de mercado na web.
> Fatos de código foram conferidos no código. Números de mercado vêm de pesquisa web de 07/10/2026, em boa parte de fontes secundárias, e não foram conferidos na origem.

## 1. Resumo

O sistema hoje atende uma instituição só e não tem nenhuma noção de tenant: nem no banco, nem no token, nem nos eventos, nem no gateway. O caminho para virar produto é este:

1. **Multi-tenancy antes de qualquer serviço novo.** As entregas E3–E5 (Node/MongoDB, C#/MySQL, Python CQRS) ficam congeladas para o produto. Cada linguagem e banco a mais multiplica o trabalho de isolamento, e o próprio vault já conclui "tenancy primeiro, decomposição depois — e é opcional".
2. **Um banco, `tenant_id` em toda tabela, com duas travas:** `@TenantId` do Hibernate e Row-Level Security do PostgreSQL. A segunda cobre os 17 relatórios em SQL cru e o serviço Python, que a primeira não alcança.
3. **Whitelabel em quatro camadas configuráveis por escola — marca, vocabulário, módulos e regras — e nada além disso.** Nenhum `if (escola == X)` no código. A organização piloto (a primeira escola a usar a plataforma) vira o tenant zero: tudo que hoje é regra fixa no código passa a ser configuração dela.
4. **Entrar no mercado pelo nicho que o modelo atual já atende** (projetos sociais, contraturno, cursos livres), e só depois construir escola seriada (ano letivo, disciplina, nota, boletim), que é onde a concorrência é mais forte e o esforço é maior.
5. **A gestão também leva marca**, em dois sentidos: o back-office da escola usa a marca da escola, e um nível "parceiro" permite que redes, consultorias e sistemas de ensino revendam o produto com a marca deles.

Esforço até o primeiro produto vendável (fases 0 a 3): **145–230 dev-dias**, cerca de 2,5 a 4 meses com três pessoas dedicadas. Detalhe e ressalvas na seção 5.

## 2. Ponto de partida

| Fato | Evidência | Consequência |
|---|---|---|
| Nenhuma noção de tenant | busca por `tenant` em Java, Python, YAML e SQL retorna zero | isolamento é o trabalho central |
| Roteamento só por path | [routes.yml](../../deploy/traefik/dynamic/routes.yml) não tem regra `Host()` | não há como distinguir escola por domínio |
| JWT HS256 com segredo compartilhado; Keycloak no compose sem uso | [security.py:7](../../services/documents/app/security.py), `JwtService.java:99` | README promete RS256 + JWKS, que não existe |
| Unicidades globais | `uq_user_email`, `uq_user_username`, `uq_person_cpf`, `uq_person_email`, `uq_role_name` em `V1__schema_inicial.sql` | a mesma pessoa não cabe em duas escolas |
| 17 relatórios em SQL cru | `reports/infra/jdbc/ReportJdbcRepository.java` | ficam fora do filtro do Hibernate; maior risco de vazamento |
| Mural e notificação buscam usuários por papel na base inteira | `MuralPostService.java:255`, `NotificationService.java:99` | com dois tenants, o post de uma escola iria para todas |
| Jobs e consumidores sem contexto | `SponsorBillingJob`, outbox, `AtestadoEventConsumer` | precisam de tenant fora de uma requisição |
| Auditoria nunca grava | pointcut `..repository.*Repository` em `AuditAspect.java:31`; pacotes reais são `infra/repositories` | sem trilha de auditoria hoje |
| Qualquer usuário logado emite declaração de qualquer aluno | `DeclarationController.java:40` (`isAuthenticated()` + `studentId` por parâmetro) | falha de autorização já existente |
| Seed roda em qualquer ambiente | `DataInicializer.java:35` sem `@Profile`, cria admin com senha conhecida | bloqueia produção |
| Marca fixa no código | `DeclarationService.java:141-153` (nome, endereço, telefone, logo), e-mails, OpenAPI, exchanges `sing.*` | precisa virar dado da organização |
| Papéis fixos | 72 `@PreAuthorize` com papel literal, um papel por usuário | sem permissão configurável por escola |
| Modelo acadêmico é de oficina e contraturno | `EnrollmentRules.java:20` (máximo de 2 matrículas), "escola principal" obrigatória | não há ano letivo, série, disciplina, nota, boletim |
| Financeiro é razão de ONG | `Sponsor`, `SponsorBillingJob`; `OVERDUE` nunca é atribuído | não há mensalidade, boleto, bolsa |
| Pessoa por herança | `Person` com `JOINED`, 1:1 obrigatório com `User`, CPF único | professor que também é responsável não pode existir |
| Testes não exercitam o PostgreSQL | H2 com Flyway desligado em `application-test.yml` | a migration de `tenant_id` não teria rede de segurança |
| Sem CI, sem `contracts/`, sem OpenTelemetry, sem frontend neste repo | conferido no repo | README descreve mais do que existe |
| Auditoria de 16/09 com 90 achados (20 críticos), nenhum corrigido | vault `sing-microservicos/auditoria/` | parte é pré-requisito de vender |

Duas correções em relação às notas do vault: o dinheiro em `FinancialEntry` e `Sponsor` já é `BigDecimal`/`DECIMAL(12,2)`; só a entidade morta `FinancialTransaction` usa `REAL`. E o frontend existe, mas no repositório do monolito (pasta `frontend`: React 18, Vite, Tailwind 4, shadcn), com a URL da API fixada em tempo de build.

## 3. Decisões que o plano assume

| # | Decisão | Alternativa descartada | Por quê |
|---|---|---|---|
| D1 | Este repositório vira a base do produto; E3–E5 congelam | seguir a trilha poliglota | cada serviço em outra linguagem e outro banco precisa reimplementar isolamento; time pequeno, sem SRE |
| D2 | `core` (Java) e `documents` (Python) permanecem; nenhum serviço novo até haver cliente pagante | reabsorver `documents` no core | já está extraído, é pequeno (1 tabela) e serve de molde de serviço satélite com tenant |
| D3 | Banco compartilhado com `tenant_id`, `@TenantId` + RLS | schema ou banco por escola | custo de operação e migração por N bancos; banco dedicado fica como opção contratual, mesmo código com outra URL |
| D4 | Tenant resolvido pelo **host** antes do login, e confirmado pela claim `tid` | resolver só pelo JWT (posição das notas do vault) | marca na tela de login exige saber a escola antes de autenticar; e-mail único por escola exige saber a escola no login |
| D5 | Autenticação própria com RS256 + JWKS publicado pelo core; Keycloak sai do compose | Keycloak com um realm por escola | menos uma peça para operar e para tematizar por escola; SSO externo entra depois, como recurso de plano |
| D6 | Usuário pertence a uma escola (`(tenant_id, email)` único) | conta global com acesso a várias escolas | mais simples e isolado; conta global é evolução, não pré-requisito |
| D7 | Frontend vem para este repo e passa a ler configuração em runtime | um build por escola | um build por escola não escala e impede autoatendimento |
| D8 | Pessoa por papéis via expand/contract (estratégia B de [analise-heranca-composicao.md](../analise-heranca-composicao.md)) | estratégia C (só papéis novos) | C não resolve professor que também é responsável, caso comum em escola |

D1, D4 e D5 contrariam o que está implementado ou anotado hoje; estão listadas de novo na seção 9 para confirmação.

## 4. Desenho-alvo

### 4.1 Hierarquia

```
plataforma (nós)
└── parceiro (opcional: rede, consultoria, sistema de ensino — revende com marca própria)
    └── organização  ← o tenant: fronteira de isolamento, de contrato e de marca
        └── unidade  ← campus ou polo; filtro dentro do tenant, não fronteira de isolamento
```

A marca resolve em cascata: organização, senão parceiro, senão plataforma. O modelo `organização → unidade → módulos ativos` já estava proposto em `sing/plano-arquitetura.md` e é reaproveitado.

### 4.2 Isolamento de dados

- Coluna `tenant_id` (UUID) em toda tabela de negócio, preenchida no backfill com a organização piloto.
- `@TenantId` filtra JPQL e Criteria e preenche a coluna no insert.
- RLS com `FORCE ROW LEVEL SECURITY` e política `tenant_id = current_setting('app.tenant_id')`. A aplicação conecta com um papel sem `BYPASSRLS`; as migrations usam outro papel.
- A variável de sessão é definida num ponto único, no `DataSource`, e por isso vale igual para JPA e para o `NamedParameterJdbcTemplate` dos relatórios. Sem contexto de tenant a política não casa com nenhuma linha: o erro é não ver nada, nunca ver demais.
- Fora de requisição: jobs iteram as organizações e abrem contexto para cada uma; consumidores leem `tenantId` do envelope do evento.
- No `documents`: mesma coluna e mesma política, chave de objeto `tenants/{tid}/documents/...`, `tid` propagado nas chamadas ao core.

### 4.3 As quatro camadas de personalização

Tudo que varia entre escolas cai em uma destas camadas. Se um pedido não cabe em nenhuma, é recurso de produto para todas ou não entra.

| Camada | O que a escola configura | O que está fixo hoje |
|---|---|---|
| **Marca** | nome, logo, cores, domínio, endereço e contato, assinatura da secretaria, remetente de e-mail, ícone do app | `DeclarationService` (cabeçalho e rodapé do PDF), e-mails de convocação e mural, título do OpenAPI, `static/imgs/logo*.png` |
| **Vocabulário** | como chama oficina/curso/disciplina, aluno/educando/atendido, padrinho/patrocinador | textos em telas, na view `V2` e nos relatórios |
| **Módulos** | psicossocial, apadrinhamento, financeiro, lista de espera, pareceres, notas | tudo sempre ligado |
| **Regras** | limite de matrículas ativas, exigência de escola de origem, janela de chamada, frequência mínima, cortes de turno | `EnrollmentRules.java:20` (2), `AttendanceService.java:277` (35 dias), `StudentsAtRiskReport.java:86` (padrão de 75%), `ShiftResolver.java:14` (12h/18h) |

**Perfis de organização** são conjuntos prontos dessas quatro camadas: projeto social, cursos livres, educação infantil, fundamental e médio. Criar uma escola é escolher um perfil e ajustar.

### 4.4 Identidade e permissões

- Token RS256 com `sub`, `uid`, `pid`, `tid`, `unidades[]`, `roles[]`. O filtro rejeita token cujo `tid` difere do tenant do host e deixa de consultar o banco a cada requisição.
- Catálogo de permissões definido no código (`matricula:escrever`, `financeiro:ler`); papéis são dados da organização, criados a partir de modelos no provisionamento; usuário pode ter vários papéis.
- Os 72 `@PreAuthorize` passam de papel literal para permissão. As três definições divergentes de "gestão" (`AttendanceService.java:224`, `MuralPostService.java:362`, `AcademicAuthorizationService.java:20`) viram uma.
- Senha padrão derivada de CPF dá lugar a convite por link de primeiro acesso.

### 4.5 Frontend e as três superfícies

| Superfície | Quem usa | Marca |
|---|---|---|
| App da escola | aluno, responsável, professor | da organização |
| Back-office da escola | secretaria, coordenação, financeiro, direção | da organização |
| Console | plataforma (cria e suspende organizações, planos, módulos, suporte com acesso auditado) e parceiro (as escolas dele) | da plataforma ou do parceiro |

Um único build. Ao carregar, o app chama `GET /api/public/tenant` (resolvido pelo host, sem login) e recebe marca, vocabulário e módulos. As cores viram variáveis CSS sobre os tokens do Tailwind 4; o vocabulário entra como sobreposição do dicionário de i18n; o menu é montado pelos módulos e permissões. O manifesto PWA é gerado por organização, então cada escola tem "seu app" instalável desde o primeiro dia, sem loja.

### 4.6 Eventos e contratos

Envelope `{eventId, eventType, version, occurredAt, tenantId, correlationId, aggregateId, payload}`, com JSON Schema versionado em `contracts/` e teste de contrato. Outbox transacional no core e no `documents`. Exchanges deixam de se chamar `sing.*`.

## 5. Fases

| Fase | Entrega | Destrava | Dev-dias |
|---|---|---|---|
| 0 | Rede de segurança | refatorar sem quebrar o comportamento atual | 15–25 |
| 1 | Saneamento | operar em produção com segurança | 40–65 |
| 2 | Multi-tenancy | segunda organização na mesma instância | 40–60 |
| 3 | Whitelabel | **vender para projeto social e contraturno** | 50–80 |
| 4 | Cobrança recorrente | vender para cursos livres e contraturno pago | 35–55 |
| 5 | Estrutura letiva | vender para infantil, depois fundamental e médio | 60–100 |
| 6 | Canal e escala | revenda whitelabel e autoatendimento | 30–50 |

As fases 0 a 2 partem das estimativas medidas no vault (Fase 0 do vault: 37–62 dev-dias; tenancy: 25–40), ajustadas pelo que já foi feito e pelo que a auditoria acrescentou. As fases 3 a 6 são ordem de grandeza e devem ser reestimadas ao fim da Fase 0. Ordem: 0 → 1 → 2 → 3; a parte de frontend da 3 pode começar durante a 2; 4 e 5 correm em paralelo depois da 2; 6 depende da 3.

### Fase 0 — Rede de segurança

- Trazer o frontend do monolito (pasta `frontend` do repositório antigo) para este repo, preservando histórico.
- CI em todo PR: build e testes do core, do `documents` e do frontend.
- Testcontainers com PostgreSQL, para que as migrations e o SQL dos relatórios rodem em teste.
- Consertar os testes que não rodam (`src/test/.../AttendanceController.java` sem sufixo `Test`; `ReportsIntegrationTest` espera 18 slugs para 17 relatórios) e criar testes para o `documents`, que não tem nenhum.
- Testes de caracterização do comportamento atual: saída dos 17 relatórios e do PDF de declaração sobre uma massa fixa.
- `CLAUDE.md` do repo com as decisões da seção 3; README alinhado ao que existe.

**Pronto quando:** nenhum PR entra sem CI verde e uma migration quebrada falha no CI.

### Fase 1 — Saneamento

- **Segurança:** seed só em perfil de desenvolvimento; sem segredo JWT default; `/internal/*` fora do gateway e com credencial de serviço; declaração checando vínculo; remover `/auth/register` público e `/h2-console`; checar usuário desativado; fechar portas de banco, broker e dashboard no compose; revogar a senha de app de e-mail exposta no histórico do repo antigo e numa nota do vault.
- **Autenticação:** RS256 + JWKS; claims autossuficientes.
- **Consistência:** outbox transacional; DLQ que de fato recebe mensagem; constraint ou lock contra duas matrículas na mesma vaga e chamada duplicada; apagar `FinancialTransaction`.
- **Auditoria:** corrigir o pointcut e a tabela `school_room`/`schoolroom` em `AuditLogService`.
- **Estrutura:** `@ManyToOne User` → `Long userId` nas 21 entidades auditáveis; remover as coleções `Student.enrollments`/`attendances`; quebrar `common → domínio`; Spring Modulith com `verify()` no CI.
- **Plataforma:** subir o Spring Boot para uma linha com suporte vigente.

**Pronto quando:** os 20 críticos da auditoria estão fechados e o `auditoria_runtime.py` do vault passa.

### Fase 2 — Multi-tenancy

- Tabelas `organizacao`, `unidade`, `organizacao_dominio`.
- `tenant_id` em todas as tabelas, backfill, unicidades compostas (as cinco globais, mais as que hoje só existem em código: nome de oficina, nome de turma, matrícula ativa).
- `@TenantId` + RLS + contexto de tenant para requisição, job e consumidor (seção 4.2).
- `tid` no token; `tenantId` no envelope; `documents` com tenant.
- Traefik com roteamento por host em subdomínio curinga e TLS.
- Provisionamento idempotente de organização (cria organização, unidade, papéis-modelo e administrador), usado igualmente por seed, testes, demonstração e onboarding. Substitui o `DataInicializer`.
- Suíte de isolamento como portão do CI (seção 6).
- Migração dos dados de produção atuais para o tenant zero.

**Pronto quando:** duas organizações convivem na mesma instância, a suíte de isolamento passa em todos os endpoints e nos 17 relatórios, e os testes de caracterização do comportamento atual seguem verdes.

### Fase 3 — Whitelabel

- Configuração por organização nas quatro camadas (seção 4.3) e `GET /api/public/tenant`.
- Frontend com configuração em runtime, tema por tokens, vocabulário, menu por módulos, manifesto PWA por organização.
- PDFs e e-mails por template com os dados da organização; remetente por organização.
- Regras lidas da configuração, com os valores atuais como configuração da organização piloto.
- Permissões e papéis por organização (seção 4.4).
- Console da plataforma.
- Conformidade mínima para tratar dado de menor: consentimento do responsável registrado, cifragem de coluna para saúde e psicossocial, auditoria de leitura desses dados, contrato de operador.
- Renomear o que carrega o nome do sistema antigo (pacote, artifact, exchanges, chave de sessão do frontend) para um codinome neutro.

**Pronto quando:** uma segunda organização com outra marca, outro vocabulário e outras regras entra no ar sem deploy e sem mudança de código, e a busca pelo nome do sistema antigo em `src/main` retorna zero.

### Fase 4 — Cobrança recorrente

- Plano de pagamento por matrícula gerando parcelas; descontos, bolsas, multa e juros; responsável financeiro distinto do responsável legal.
- `Sponsor` vira um tipo de pagador, e o apadrinhamento continua funcionando para a organização piloto.
- Porta `PaymentProvider` com um adaptador de PSP, subconta por organização e split (Pix e boleto), webhooks idempotentes e baixa automática.
- Régua de cobrança por e-mail e inadimplência real (`OVERDUE` passa a ser atribuído).
- Portal do responsável: segunda via e histórico.

**Pronto quando:** uma organização cobra uma mensalidade de ponta a ponta em sandbox, com baixa por webhook.

### Fase 5 — Estrutura letiva

- Pessoa por papéis (D8), com delegação para preservar os ~128 pontos que chamam getters herdados.
- Ano e período letivo, calendário com feriados e dias letivos, etapa e série, turma vinculada ao ano, rematrícula, número de matrícula.
- `Workshop` generalizado para componente curricular; turma × componente × professor; grade horária por slot.
- Avaliação plugável por perfil: parecer descritivo (já existe) atende educação infantil; notas, média, recuperação, boletim e histórico atendem fundamental e médio.
- Ocorrências disciplinares.
- Exportação no leiaute do Educacenso.

A fase tem um corte intermediário vendável: ano letivo + etapas + parecer já servem à educação infantil, antes de notas e boletim.

**Pronto quando:** uma escola de fundamental fecha um bimestre e emite boletim.

### Fase 6 — Canal e escala

- Nível parceiro, console do parceiro com a marca dele, apuração de comissão.
- Onboarding por autoatendimento e importador de planilha (alunos, responsáveis, turmas).
- Cobrança do próprio SaaS: planos e medição de alunos ativos.
- Domínio próprio da escola com TLS automático; app publicado nas lojas como adicional pago.
- WhatsApp como canal (as filas `whatsapp.queue` e `notification.queue` existem sem consumidor).
- Canal do titular e exportação de dados por titular.

## 6. Ideias para o desenvolvimento

1. **Suíte de isolamento gerada do OpenAPI.** Cria duas organizações, percorre todos os endpoints e todos os relatórios com o token de uma e falha se aparecer qualquer dado da outra. Endpoint novo entra na suíte sozinho.
2. **Testes de arquitetura como regra executável:** toda entidade tem tenant; toda tabela de negócio tem política RLS (consulta a `pg_policies`); nenhum literal com o nome do sistema antigo em `src/main`; limites de módulo via Spring Modulith.
3. **tenant zero idêntico ao comportamento atual como critério de aceite** de cada fase, sustentado pelos testes de caracterização da Fase 0. É o que impede a refatoração de quebrar o único cliente real.
4. **Cliente TypeScript gerado do OpenAPI,** eliminando a divergência entre `axios` e `fetch` e os contratos mantidos à mão.
5. **Organização de demonstração** com dados sintéticos, recriada toda noite pelo mesmo provisionamento. Serve a vendas e a testes de ponta a ponta.
6. **Observabilidade por tenant:** `tenant_id` nos logs e nas métricas, limite de requisições por organização no gateway. Mostra custo por cliente e vizinho barulhento.
7. **Importação e exportação completas desde cedo.** Importar reduz o custo de entrada do cliente; exportar relacional e completo é argumento de venda (seção 7.3).
8. **Backup com restauração por organização testada,** porque um cliente vai pedir para desfazer um erro só dele.
9. **Decisões no `CLAUDE.md` do repo.** Quatro arquivos citam `docs/adr/`, que não existe, e o planejamento mora fora do repositório.

## 7. Ideias de produto e negócio

### 7.1 Sequência de mercado

| Marco | Segmento | Por que nesta ordem |
|---|---|---|
| Após Fase 3 | Projetos sociais e contraturno gratuito | o modelo atual (oficina, lista de espera, psicossocial, padrinhos) já é este; a organização piloto é o caso de referência; a pesquisa não encontrou software comercial específico para OSC com chamada e relatório a financiador |
| Após Fase 4 | Cursos livres e contraturno pago (esporte, música, reforço, idiomas) | mesmo modelo acadêmico, faltando só cobrar mensalidade |
| Fase 5, corte intermediário | Educação infantil pequena | avalia por parecer, que já existe; setor sob pressão financeira (matrículas privadas em queda em 2025) |
| Fim da Fase 5 | Fundamental e médio com menos de 200 alunos | cerca de 28 mil das 41,7 mil escolas privadas; incumbentes miram 200+ ou 300+ alunos |
| Após Fase 6 | Redes, consultorias e sistemas de ensino como revendedores | a pesquisa não encontrou ERP escolar brasileiro vendido como whitelabel para revenda |

Ressalva sobre o primeiro marco: projeto social paga pouco. Ele vale como referência, como validação do multi-tenant com clientes tolerantes e como porta para financiadores, não como fonte principal de receita.

### 7.2 Monetização

- **Assinatura com preço público,** por aluno ativo com piso mensal. Quase todos os concorrentes operam "sob consulta"; os poucos que publicam ficam entre R$ 1 e R$ 5 por aluno/mês ou R$ 80 a R$ 850 por escola/mês.
- **Faixa gratuita ou subsidiada para OSC,** financiada pelos demais planos ou por patrocínio de institutos. Gera casos e base instalada.
- **Receita sobre pagamentos** via PSP com split. No mercado, a camada de pagamento rende mais que a assinatura. Garantia de mensalidade e antecipação de recebíveis ficam fora: exigem capital e crédito, e os dois nomes do segmento (isaac e Educbank) pertencem a grupos grandes.
- **Adicionais:** app com a marca da escola nas lojas, domínio próprio, implantação assistida, módulos (psicossocial, prestação de contas).
- **Revenda:** comissão recorrente ao parceiro. A referência encontrada em ERP genérico é 30%.

### 7.3 Diferenciais a partir do que já existe

- **Prestação de contas a financiador.** Os 17 relatórios e o cadastro de padrinhos viram um módulo de relatório de impacto (atendidos, frequência, perfil) e um portal do padrinho. Em OSC é o que o gestor mais precisa e o que a pesquisa não encontrou pronto.
- **Captação e fila de espera.** Pré-cadastro, lista de espera por oficina e turno e convocação automática por vaga já existem. Os incumbentes vendem CRM de captação como diferencial.
- **Alerta de evasão por regra.** O relatório de alunos em risco já calcula frequência abaixo do limiar; falta avisar coordenação e responsável de forma proativa. Não depende de IA.
- **Conformidade como produto.** Consentimento, auditoria de leitura de dado sensível, relatório de impacto pronto e canal do titular. O acompanhamento psicossocial torna isso mais valioso, e a ANPD já advertiu órgão público por expor dados de saúde de crianças.
- **Portabilidade e suporte.** Exportação completa e relacional a qualquer momento e suporte por WhatsApp com prazo. Há relatos públicos de escolas que receberam CSV solto e tiveram o sistema bloqueado ao trocar de fornecedor.
- **Educacenso.** A segunda etapa do censo vai de fevereiro a março de 2027; a exportação pronta é um prazo comercial natural para a Fase 5.

### 7.4 O que deixar de fora

- Os 13 módulos do plano SING ao mesmo tempo. O próprio plano avisa: "sem cliente, não invente requisito".
- Customização por cliente em código.
- IA que perfila alunos. O ECA Digital (Lei 15.211/2025, em vigor desde março de 2026) alcança produto de acesso provável por menores; se um ERP escolar entra no escopo é incerto e pede parecer jurídico. Um assistente de redação de parecer para o professor, opcional e com o professor revisando, é candidato para depois desse parecer.
- Catraca, reconhecimento facial e cantina.

## 8. Riscos

| Risco | Mitigação |
|---|---|
| Vazamento entre escolas, com dado de menor e psicossocial | duas travas (4.2) e suíte de isolamento como portão do CI |
| Refatoração quebra o comportamento atual | testes de caracterização e "tenant zero idêntico" em cada fase |
| Virar fábrica de software sob medida | regra das quatro camadas (4.3) |
| Construir escola seriada sem cliente | sequência de mercado (7.1): só entra depois de vender ao nicho atual |
| Time pequeno com duas linguagens | congelar novos serviços (D1, D2); reavaliar o `documents` se o custo aparecer |
| Mercado encolhendo e consolidando | preço público, nicho mal atendido, importadores para absorver base de quem sai |
| Dependência de uma ou duas pessoas | decisões no `CLAUDE.md`, CI, testes de arquitetura |

## 9. Em aberto

1. **Este repositório é a base do produto?** E o requisito acadêmico das entregas E3–E5 ainda existe? Se existir, o plano propõe mantê-lo numa branch separada.
2. **Onde o sistema atual roda em produção hoje?** As notas indicam o monolito antigo em SQL Server no Azure. Isso define a migração para o tenant zero na Fase 2.
3. **Primeiro segmento:** confirmar a entrada por projeto social e contraturno, ou ir direto para escola seriada, o que antecipa a Fase 5 e adia a primeira venda.
4. **Tamanho e dedicação do time,** que convertem dev-dias em calendário.
5. **Nome do produto** e relação com o SING, que nas notas é uma plataforma mais ampla (escola + apadrinhamento + acolhimento) pensada para incubadora.
6. **Keycloak:** remover (D5) ou adotar.
7. **Tenant pelo host (D4)** em vez de só pelo JWT, como está nas notas.
