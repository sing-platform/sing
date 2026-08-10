# SGS-CADI — Microserviços Poliglotas

Refatoração do [SGS-CADI](https://github.com/JoaoVictorCarvalho07/sgs-cadi) (monolito Spring Boot)
para uma arquitetura de **microserviços poliglotas** — linguagens e bancos de dados heterogêneos.

Projeto acadêmico. O planejamento completo está no vault Obsidian, módulo `cadi-microservicos/`.

## Arquitetura alvo

| Serviço | Linguagem | Banco | Papel |
|---|---|---|---|
| **core** | Java 21 / Spring Boot | PostgreSQL | Domínio acadêmico (pessoas, turmas, matrículas, chamada). Indivisível — 47% do código original |
| **documents** | Python / FastAPI | PostgreSQL + MinIO | Upload, atestados, fluxo de aprovação |
| **communication** | Node 20 / NestJS | MongoDB | Notificações, mural |
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
contracts/      contratos compartilhados (proto/ e events/) — fonte da verdade entre serviços
deploy/         docker-compose, Traefik, observabilidade
docs/           decisões e notas de implementação
```

## Entregas

| # | Entrega | Status |
|---|---|---|
| **E1** | Infra base (compose, Traefik, Keycloak, OTel) + core migrado para PostgreSQL | 🚧 em andamento |
| E2 | `documents` (Python) | ⬜ |
| E3 | `communication` (Node/TS + MongoDB) | ⬜ |
| E4 | `finance` (C#/.NET + MySQL) | ⬜ |
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
