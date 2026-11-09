# Deploy — stack local (E1)

Infra base da arquitetura poliglota. Nesta entrega sobem: **Traefik**, **PostgreSQL**,
**core** (Java), **RabbitMQ**, **MinIO** e **Keycloak**. Observabilidade é opcional.

## Subir

```bash
cp .env.example .env
```

Edite o `.env` — três valores **não** têm default e o compose falha sem eles:
`JWT_SECRET`, `MINIO_PASSWORD`, `KEYCLOAK_PASSWORD`.

```bash
openssl rand -hex 28    # use a saída como JWT_SECRET
```

São 56 caracteres hex (42 bytes): com esse tamanho o core assina o token em HS256, o único
algoritmo que o documents aceita. Um segredo maior faz o core assinar em HS384 ou HS512 e o
documents recusar todos os tokens.

```bash
docker compose up -d --build
```

O primeiro build baixa as dependências Maven e leva alguns minutos.

Com observabilidade (Jaeger + Prometheus):

```bash
docker compose --profile observability up -d
```

## Endpoints

| Serviço | URL |
|---|---|
| API (via Traefik) | http://localhost:8080/api |
| Health | http://localhost:8080/actuator/health |
| Dashboard Traefik | http://localhost:8081 |
| RabbitMQ | http://localhost:15672 |
| MinIO console | http://localhost:9001 |
| Keycloak | http://localhost:8180 |
| Notificação da matrícula | http://localhost:8080/api/v1/comunicacao/notificacoes/{eventId} |
| Jaeger | http://localhost:16686 |
| Prometheus | http://localhost:9090 |

## Documentação (Swagger / OpenAPI)

A UI é pública no gateway. O negócio continua com o JWT que já tinha.
No Swagger, **Try it out** chama o mesmo host (Traefik local ou o Ingress no EKS).
Não cole um segredo na página: o botão Authorize só guarda o token no browser.

| Serviço | UI (pública) | Schema (público) | Negócio |
|---|---|---|---|
| core | http://localhost:8080/swagger-ui/index.html | http://localhost:8080/v3/api-docs | `/api/**` com JWT |
| finance | http://localhost:8080/finance/swagger | http://localhost:8080/finance/swagger/v1/swagger.json | `/api/v1/finance/**` com JWT. `POST /cobrancas` não passa pelo gateway |
| documents | http://localhost:8080/documents/docs | http://localhost:8080/documents/openapi.json | `/api/documents/**` com JWT. `/api/documents/docs` não é a UI: cai na rota `/{document_id}` |
| communication | http://localhost:8080/comunicacao/docs | http://localhost:8080/comunicacao/openapi.json | `/api/v1/mural/**` e `/api/v1/notifications/**` com JWT. `GET /api/v1/comunicacao/notificacoes/{eventId}` segue sem JWT |

`GET http://localhost:8080/api/v1/instancia` também é público. Devolve o nome real
da instância do core (`servico` + `instancia`) e o header `X-Pod`. No compose há
um container só. Três nomes diferentes aparecem no Ingress, com 3 réplicas, e o
balanceamento não é round-robin estrito.

## Verificar

```bash
docker compose ps
curl -fsS http://localhost:8080/actuator/health
docker compose logs -f core
```

O health deve retornar `{"status":"UP"}`. RabbitMQ e mail estão fora do health check por
padrão (`HEALTH_RABBIT_ENABLED` / `HEALTH_MAIL_ENABLED`), então um broker indisponível
não derruba o status do serviço.

## Matrícula criada (N2)

O compose sobe o Mongo do comprovante, o Mongo da comunicação, o serviço Nest e o finance (.NET + MySQL).

Testes do fluxo, na raiz do repositório:

```bash
(cd services/core && mvn -B test -Dtest=MatriculaEnvelopeTest,MatriculaIntegracaoWorkerTest,CobrancaClientTest,EnrollmentServiceTest,EnrollmentControllerWebMvcTest)
(cd services/documents && python -m unittest tests.test_comprovante)
(cd services/communication && npm test)
```

## Operação

```bash
docker compose restart core
docker compose down              # para tudo, preserva volumes
docker compose down -v           # APAGA os bancos
docker compose logs -f postgres-core
```

Acessar o banco do core:

```bash
docker compose exec postgres-core psql -U sing -d sing_core -c '\dt'
```

## Notas

- **Healthcheck com `depends_on: service_healthy`** é obrigatório aqui: sem isso o Spring
  sobe antes do PostgreSQL aceitar conexão e o container morre no boot. O RabbitMQ precisa
  de `start_period: 90s` — leva ~60s para subir e sem carência o Docker conta as falhas de
  boot como reais.
- **Traefik roteia por path** (`/api`, `/swagger-ui`, `/actuator`) via **file provider**,
  em `traefik/dynamic/routes.yml`. Serviços novos entram editando esse arquivo (recarrega
  sozinho, sem restart). Não usamos o docker provider: ele exige montar `/var/run/docker.sock`
  no gateway — acesso root ao host — e o socket do Docker Desktop 29.x não responde a
  clientes em container.
- O core roda com `ddl-auto: validate`: o schema vem das migrations Flyway em
  `services/core/src/main/resources/db/migration/postgresql`, nunca do Hibernate.
