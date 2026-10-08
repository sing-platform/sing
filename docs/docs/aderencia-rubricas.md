# Aderência às rubricas

Onde cada critério das três rubricas é atendido. "Seção" se refere a `SING - Especificacao do Projeto.docx`. Para os critérios de implementação do TCC 2, a coluna Situação resume o que a análise do repositório encontrou em 07/10/2026 e o que ainda falta demonstrar.

A equipe tem 4 integrantes, então os itens que dependem da quantidade de integrantes têm no mínimo 16.

## Especificação de Software — RA1 (prazo 10/10/2026)

| # | Critério | Onde | Quantidade |
|---|---|---|---|
| 1 | Quadro “3 Objetivos” | Seção 1 | 3 objetivos com problema, valor e métrica |
| 2 | É – Não é – Faz – Não faz | Seção 2 | 4 quadrantes, 4 a 5 itens cada |
| 3 | Visão do produto | Seção 3 | Problemas, Expectativas e os 5 campos |
| 4 | BPMN “to be” | Seção 4 | 2 pools, 4 raias, gateways exclusivos e paralelos, mensagens, temporizador |
| 5 | Atores | Seção 5 | 10 atores (8 humanos, 2 sistemas externos) |
| 6 | Requisitos funcionais | Seção 6 | 36 RF (12 do módulo escolar), com ator, objetivo, prioridade, justificativa e sprint |
| 7 | Estórias de usuário | Seção 7 | 16 estórias, 45 critérios DADO QUE / QUANDO / ENTÃO |
| 8 | Requisitos não funcionais | Seção 8 | 19 RNF mensuráveis, com característica da ISO/IEC 25010 |
| 9 | Diagrama geral de casos de uso | Seção 9 | 36 casos (um por RF) em cinco diagramas: geral, um por módulo (plataforma, acadêmico, gestão) e módulo escolar; generalização, «include» e «extend» |
| 10 | Especificações de caso de uso | Seção 10 | 16 casos completos, com protótipo de tela |
| 11 | Diagrama de atividades | Seção 11 | Raias, decisões com guarda, fork/join |

## Especificação de Software — RA2 (prazo 28/11/2026)

| # | Critério | Onde |
|---|---|---|
| 1 | Ajustes do RA1 | Depende da correção do RA1 |
| 12 | Modelo de dados | Seção 12: 5 diagramas lógicos relacionais em UML, 42 tabelas |
| 13 | Diagrama de classes | Seção 13: 3 diagramas em nível técnico (camadas api, application, domain, infra) |
| 14 | Diagramas de sequência | Seção 14: 16, um por caso de uso especificado |
| 15 | Casos de teste | Seção 15 e `casos-de-teste.xlsx`: 45 casos, um por critério de aceite |

## TCC 1 — documentação por sprint

| Critério | Onde | Situação |
|---|---|---|
| Melhorias em 3 Objetivos, É/Não é, Problemas e Expectativas, Visão | Seções 1 a 3 | Pendente da correção do TDE1 |
| RF priorizados para a Sprint 1 | Seção 6 (linhas sombreadas) | Coberto: 8 RF na Sprint 1, incluindo login |
| Estórias da Sprint 1 com critérios de aceite | Seção 7 | Coberto: US001 a US008 |
| Modelo de dados da Sprint 1 (≥3 entidades) | Seção 12 | Coberto |
| Estórias da Sprint 2 | Seção 7 | Coberto: US009 a US016 |
| Modelo de dados da Sprint 2 | Seção 12 | Coberto |
| Analytics: objetivo, dados coletados, protótipo com filtro de período | `analytics.md` e protótipo P016 | Coberto |

Os critérios de defesa (incremento funcionando, login com dados persistidos, identidade visual, validação de campos, campos obrigatórios marcados) são demonstrados no sistema. Os protótipos da seção 10 já mostram marcação de obrigatórios, textos de ajuda e mensagens de validação para servir de referência.

## TCC 2 — implementação

| Critério | Pontos | Situação encontrada | O que falta demonstrar |
|---|---|---|---|
| Interface separada do backend, JSON assíncrono | 0,5 | SPA React consumindo API REST, no repositório do monolito | Trazer o frontend para este repositório |
| CRUD | 0,5 | Alunos, responsáveis, turmas, oficinas e matrículas existem | — |
| Relatório com gráfico e filtros | 0,5 | 17 relatórios tabulares; painel com gráficos é o RF21 | Implementar o painel do protótipo P016 |
| Persistência em ≥4 tabelas, relações, índices, validação | 0,5 | 31 tabelas, chaves estrangeiras e índices nas migrations | — |
| Processo ágil documentado | 0,5 | Sprints atribuídas aos RF (seção 6) | Planejamento, backlog e revisão de cada sprint |
| Uso do GitHub | 0,5 | Repositório com commits convencionais | Branch por desenvolvedor, tags semânticas por sprint, README |
| Requisitos formais e qualidade | 1,0 | Critérios BDD e casos de teste nesta especificação | Executar os critérios com Cucumber ou equivalente e registrar o plano de testes |
| Publicado na nuvem | 1,0 | Só docker-compose local neste repositório | Publicar backend e frontend |
| JWT com ≥2 permissões | 1,0 | JWT com 9 papéis | RS256 e permissões por organização (RNF2) |
| Verbos e códigos HTTP | 1,0 | Controllers REST com tratamento de exceções | — |
| Cobertura de testes do backend ≥75% | 1,0 | 226 testes, sem medição de cobertura | Configurar JaCoCo e alcançar 75% (RNF16) |
| Cobertura de testes do frontend ≥75% | 1,0 | Não encontrada | Testes e medição no frontend |
| Avaliação de usabilidade | 1,0 | Não encontrada | Teste com usuário ou checklist, com registro |
| Features e user stories documentadas | 1,0 | Seções 6, 7 e 10 | — |
| Serviços documentados em OpenAPI | 1,0 | springdoc gera a documentação automática | Descrições, erros e exemplos reais em 80% dos serviços (RNF18) |
| Internacionalização em ≥3 páginas | 1,5 | i18n só no módulo do aluno | Estender a 3 páginas, com datas e moeda (RNF14) |
| Responsivo e modo escuro | 1,5 | Telas responsivas; modo escuro não encontrado | Modo escuro (RNF12) |
| Microsserviços com troca de mensagens | 1,5 | `core` e `documents`, banco próprio cada um, eventos via RabbitMQ | — |
| Índices e consultas dinâmicas em ≥3 relatórios | 1,5 | 17 relatórios com SQL parametrizado | — |
| Upload com armazenamento e acesso controlado | 1,5 | Serviço `documents` com MinIO e autorização por papel | CDN, para a nota máxima |
| UML: classes, 2 sequências, atividades | 1,5 | Seções 11, 13 e 14 | — |
| Tecnologia extra | 0,5 a 1,5 | Multi-tenancy com segurança em nível de linha, RabbitMQ, OpenTelemetry previsto | Validar com os professores até a 3ª sprint |
