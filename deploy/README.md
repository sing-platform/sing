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
openssl rand -hex 48    # use a saída como JWT_SECRET
```

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
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Dashboard Traefik | http://localhost:8081 |
| RabbitMQ | http://localhost:15672 |
| MinIO console | http://localhost:9001 |
| Keycloak | http://localhost:8180 |
| Jaeger | http://localhost:16686 |
| Prometheus | http://localhost:9090 |

## Verificar

```bash
docker compose ps
curl -fsS http://localhost:8080/actuator/health
docker compose logs -f core
```

O health deve retornar `{"status":"UP"}`. RabbitMQ e mail estão fora do health check por
padrão (`HEALTH_RABBIT_ENABLED` / `HEALTH_MAIL_ENABLED`), então um broker indisponível
não derruba o status do serviço.

## Operação

```bash
docker compose restart core
docker compose down              # para tudo, preserva volumes
docker compose down -v           # APAGA os bancos
docker compose logs -f postgres-core
```

Acessar o banco do core:

```bash
docker compose exec postgres-core psql -U cadi -d cadi_core -c '\dt'
```

## Notas

- **Healthcheck com `depends_on: service_healthy`** é obrigatório aqui: sem isso o Spring
  sobe antes do PostgreSQL aceitar conexão e o container morre no boot.
- **Traefik roteia por path** (`/api`, `/swagger-ui`, `/actuator`). Serviços novos entram
  declarando labels no próprio compose — sem arquivo de configuração central.
- O core roda com `ddl-auto: validate`: o schema vem das migrations Flyway em
  `services/core/src/main/resources/db/migration/postgresql`, nunca do Hibernate.
