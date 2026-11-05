# SING — Plataforma whitelabel de gestão escolar

Uma mesma plataforma atende várias escolas, cada uma com sua marca, vocabulário, módulos e regras,
implementada como **microserviços poliglotas** — linguagens e bancos de dados heterogêneos.

A documentação está em `docs/docs/`.

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
contracts/      contrato fechado entre serviços; hoje só o N2 da matrícula
deploy/         docker-compose, Traefik, observabilidade
docs/           decisões e notas de implementação
```

## Contrato N2

O recorte fechado da matrícula criada está só em [contracts/n2-matricula.md](contracts/n2-matricula.md): envelope, filas, cobrança, isolamento do documents e falhas. A tabela de arquitetura acima descreve o alvo amplo (E1–E5). Ela não substitui esse contrato, e o contrato não troca o documents já entregue nem implementa serviço nenhum.

## Entregas

| # | Entrega | Status |
|---|---|---|
| **E1** | Infra base (compose, Traefik, Keycloak, OTel) + core migrado para PostgreSQL | ✅ concluída |
| **E2** | [`documents`](services/documents) (Python + PostgreSQL + MinIO), evento `AtestadoApproved` | ✅ concluída |
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
