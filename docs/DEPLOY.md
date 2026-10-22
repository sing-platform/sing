# Deploy em produção

VM Oracle ARM64 (Ampere): 2 vCPU, 11 GiB RAM, Ubuntu 24.04, Docker 29, Compose v5.

| | |
|---|---|
| Host | `147.15.96.215` |
| Usuário | `deploy` |
| Diretório | `/opt/sing` |

As portas **8080** e **8081** do host já estão ocupadas. O Traefik publica só
`127.0.0.1:8084` (HTTP) e `127.0.0.1:8085` (dashboard). Postgres, RabbitMQ,
MinIO e Keycloak não publicam porta no host. Não há domínio ainda.

## Pipelines

- **CI** (pull request e push em `main`): `mvn test` no core, `pytest` no documents, build das duas imagens. O repositório recusa `uses:` de actions de terceiros na validação, então o workflow não usa `actions/checkout` nem o cache do Actions; Maven e pip baixam as dependências em cada run.
- **CD** (push em `main`, depois do CI verde, environment `production`): build `linux/arm64` com buildx/QEMU, push para `ghcr.io/sing-platform/core` e `ghcr.io/sing-platform/documents` (tags `<sha>` e `latest`), depois SSH.

O job de CD não pede TTY (`ssh -nT`, `RequestTTY=no`, sem `-t`), não encaminha agent nem X11 e não usa sudo. A chave em `authorized_keys` precisa destas restrições:

```
no-pty,no-agent-forwarding,no-X11-forwarding
```

A aprovação manual fica no environment `production` (Required reviewers). O job inteiro — build, push e deploy — só começa depois da aprovação.

## Segredos

Grave no environment **production** (Settings → Environments), não no código.

| Secret | Valor |
|---|---|
| `DEPLOY_HOST` | `147.15.96.215` |
| `DEPLOY_USER` | `deploy` |
| `DEPLOY_PATH` | `/opt/sing` |
| `DEPLOY_SSH_KEY` | chave privada completa, com as linhas `BEGIN`/`END` |

`GITHUB_TOKEN` é automático; o workflow pede `packages: write` para o GHCR.

Antes do merge: crie o environment `production`, marque **Required reviewers** e salve os quatro secrets. Sem os reviewers o primeiro deploy segue sozinho assim que o CI da `main` passa.

## Servidor, uma vez

Isto é feito no console da VM (ou por um usuário com shell). O job de CD não executa estes comandos e não tem sudo.

```bash
sudo adduser --disabled-password --gecos "" deploy
sudo usermod -aG docker deploy
sudo mkdir -p /opt/sing
sudo chown deploy:deploy /opt/sing
sudo -u deploy mkdir -p /home/deploy/.ssh /opt/sing/traefik/dynamic
sudo -u deploy chmod 700 /home/deploy/.ssh
```

Chave (na máquina de quem administra, não no repo):

```bash
ssh-keygen -t ed25519 -C github-actions-deploy -f sing_deploy
```

A pública entra em `/home/deploy/.ssh/authorized_keys`, numa linha só, com as restrições. A privada é o secret `DEPLOY_SSH_KEY`. Não commite nenhuma das duas.

```
no-pty,no-agent-forwarding,no-X11-forwarding ssh-ed25519 AAAA... github-actions-deploy
```

Login no GHCR, como o usuário `deploy` (o token precisa de `read:packages`). Sem prompt, porque essa chave não abre TTY:

```bash
printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u SEU_USUARIO --password-stdin
```

Confira que `docker info` funciona sem sudo numa sessão nova do `deploy` (o grupo `docker` só vale depois de relogar).

## `.env` do servidor

Fica em `/opt/sing/.env`, modo `600`, dono `deploy`. Não é versionado e o CD não sobrescreve.

Obrigatórios (o compose falha sem eles):

```bash
openssl rand -hex 48   # JWT_SECRET
```

```
JWT_SECRET=
MINIO_PASSWORD=
KEYCLOAK_PASSWORD=
CORE_DB_PASSWORD=
DOCS_DB_PASSWORD=
RABBITMQ_PASSWORD=
CORS_ORIGINS=http://localhost:8084
```

`MINIO_PASSWORD` com no mínimo 8 caracteres. Não defina portas de host: o override fixa o Traefik e remove o resto. `IMAGE_TAG` é injetada pelo CD; no rollback, exporte o sha antigo.

Nomes com default, se quiser trocar: `CORE_DB_NAME`, `CORE_DB_USER`, `DOCS_DB_NAME`, `DOCS_DB_USER`, `DOCS_BUCKET`, `MINIO_USER`, `KEYCLOAK_ADMIN`, `RABBITMQ_USER`, `EVENTS_EXCHANGE`.

## O que o CD copia

Para `/opt/sing`: `docker-compose.yml`, `docker-compose.prod.yml` e `traefik/dynamic/routes.yml`. Em seguida, sem TTY e sem sudo:

```bash
export COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml
export IMAGE_TAG=<sha do commit>
docker compose pull && docker compose up -d
docker image prune -f
```

`prune -f` só apaga imagem sem tag. As tags antigas ficam no host para rollback.

Limites de memória da stack (sem observabilidade): cerca de 4,4 GiB. Core e Keycloak sobem com `-Xmx512m`.

## Acesso

A chave de deploy não abre shell. O túnel tem que ser sem TTY:

```bash
ssh -N -T -L 8084:localhost:8084 deploy@147.15.96.215
```

Dashboard, se precisar: acrescente `-L 8085:localhost:8085`.

- API: http://localhost:8084/api
- Health: http://localhost:8084/actuator/health

Quando houver domínio, um bloco Caddy **no host** faz proxy do subdomínio para `127.0.0.1:8084`. Nada disso entra neste repositório ainda.

## Rollback

No servidor, com o sha de um push anterior (está na tag da imagem no GHCR):

```bash
cd /opt/sing
export COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml
export IMAGE_TAG=<sha>
docker compose pull && docker compose up -d
```

Isso troca as imagens `core` e `documents`. O `latest` no registry continua no último CD que publicou; o rollback não move essa tag. Os arquivos de compose no servidor permanecem os do deploy atual.
