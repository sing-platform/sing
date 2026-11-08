# SING — Plataforma whitelabel de gestão escolar

Uma mesma plataforma atende várias escolas, cada uma com sua marca, vocabulário, módulos e regras,
implementada como **microserviços poliglotas** — linguagens e bancos de dados heterogêneos.

A documentação está em `docs/docs/`.

## Arquitetura alvo

| Serviço | Linguagem | Banco | Papel |
|---|---|---|---|
| **core** | Java 21 / Spring Boot | PostgreSQL | Domínio acadêmico (pessoas, turmas, matrículas, chamada). Indivisível — 47% do código original |
| **documents** | Python / FastAPI | PostgreSQL + MinIO | Upload, atestados, fluxo de aprovação |
| **communication** | Node 20 / NestJS | MongoDB | Mural, notificações in-app, notificação da matrícula |
| **finance** | C# / .NET 8 | MySQL | Transações, patrocinadores, cobrança |
| **reports** | Python / FastAPI | PostgreSQL (read model) | Relatórios via CQRS |

**4 linguagens · 3 bancos.**

```
Internet ──▶ Traefik ──┬──▶ core        (Java)    ──▶ PostgreSQL
                       ├──▶ documents   (Python)  ──▶ PostgreSQL + MinIO
                       ├──▶ communication (Node)  ──▶ MongoDB
                       ├──▶ finance     (C#)      ──▶ MySQL
                       └──▶ reports     (Python)  ──▶ PostgreSQL (read model)
                                  │
                            RabbitMQ (eventos)
                                  │
                    OpenTelemetry ──▶ Jaeger / Prometheus / Grafana
```

## Estrutura

```
services/       um diretório por serviço, cada um com seu Dockerfile e migrations
contracts/      contrato fechado entre serviços; hoje só o N2 da matrícula
deploy/         docker-compose, Traefik, observabilidade
docs/           decisões e notas de implementação
```

## Contrato N2

O recorte fechado da matrícula criada está em [contracts/n2-matricula.md](contracts/n2-matricula.md). O core grava a matrícula e a pendência na mesma transação, publica `matricula.criada` e chama `POST /cobrancas`. O documents e o communication consomem filas separadas, cada um no próprio Mongo. O finance (.NET + MySQL) atende `POST /cobrancas` e os recursos em `/api/v1/finance/*`; o core não tem mais API financeira.

O mural e as notificações in-app moram no communication ([contracts/mural-notificacoes.md](contracts/mural-notificacoes.md)). Os caminhos `/api/v1/mural` e `/api/v1/notifications` não mudaram; o gateway os manda ao communication.

## Entregas

| # | Entrega | Status |
|---|---|---|
| **E1** | Infra base (compose, Traefik, Keycloak, OTel) + core migrado para PostgreSQL | ✅ concluída |
| **E2** | [`documents`](services/documents) (Python + PostgreSQL + MinIO), evento `AtestadoApproved` | ✅ concluída |
| **E3** | [`communication`](services/communication) (Node/TS + MongoDB), mural e notificações in-app | ✅ concluída |
| **E4** | [`finance`](services/finance) (C#/.NET + MySQL), `POST /cobrancas` | ✅ concluída |
| E5 | `reports` (Python + read model CQRS) | ⬜ |

## Princípios

- **Database-per-service** — nenhum serviço lê a tabela do outro.
- **Consistência forte fica no core** — vaga, matrícula, chamada e declaração oficial não são distribuídas.
- **Eventual consistency** para notificação, mural, atestado, relatórios e auditoria.
- **JWT RS256 + JWKS** — serviços recebem só a chave pública; nenhum deles pode emitir token.

## Como rodar

```bash
cd deploy
cp .env.example .env
docker compose up -d
```

Detalhes em [deploy/README.md](deploy/README.md).

A UI de cada serviço passa pelo mesmo gateway (`localhost:8080` no compose, o Ingress no EKS). Ela é pública. As rotas de negócio do core, do documents, do finance e do communication continuam pedindo JWT.

| Serviço | UI |
|---|---|
| core | http://localhost:8080/swagger-ui/index.html |
| finance | http://localhost:8080/finance/swagger |
| documents | http://localhost:8080/documents/docs |
| communication | http://localhost:8080/comunicacao/docs |

`GET /api/v1/instancia` (público) diz qual instância do core respondeu. No compose é uma só.
