# cadi-documents

Serviço de documentos: upload, download, atestados e fluxo de aprovação.
**Python 3.12 / FastAPI · PostgreSQL próprio · MinIO (S3)**

Extraído do módulo `document` do monolito — o primeiro candidato, porque já
referenciava pessoas e usuários por id (nunca `@ManyToOne`), o storage já era
externo e a única ponte com o domínio acadêmico estava isolada numa classe.

## O que mudou em relação ao monolito

| Antes (core Java) | Agora |
|---|---|
| Tabela `user_documents` no banco do core | Banco próprio (`cadi_documents`) |
| Azure Blob Storage | MinIO / S3 |
| Detecção de MIME com Apache Tika | `python-magic` (libmagic) |
| Aprovar atestado **escrevia** em `attendance` | Publica evento `AtestadoApproved` |
| `AtestadoApprovalService` chamado direto | Core consulta via HTTP + consome evento |

## Endpoints

Prefixo `/api/documents` — o mesmo do monolito, então o frontend não muda.

| Método | Rota | Quem |
|---|---|---|
| POST | `/me` | aluno (atestado/justificativa) |
| POST | `/student/{id}` | responsável |
| POST | `/person/{id}` | gestão |
| POST | `/patient/{id}/therapeutic` | psicólogo |
| GET | `/me`, `/student/{id}`, `/person/{id}` | conforme perfil |
| GET | `/patient/{id}/therapeutic` | psicólogo autor |
| GET | `/atestados`, `/atestados/pending` | gestão |
| PATCH | `/{id}/approve`, `/{id}/reject` | gestão |
| GET | `/{id}/download` | dono / gestão |
| PUT/DELETE | `/{id}` | dono / gestão |
| GET | `/internal/covering` | **core** (interno) |

## Regras preservadas do monolito

- **10 MB** por arquivo; apenas `application/pdf`, `image/jpeg`, `image/png`, `image/webp`.
- **MIME detectado por magic bytes**, nunca pelo `Content-Type` do cliente — que é
  informado por quem envia e pode ser forjado.
- **Ação compensatória**: se a gravação no banco falhar após o upload, o objeto é
  removido do storage (evita arquivo órfão).
- **Acesso**: anotação terapêutica só para o psicólogo que enviou (por `user_id`);
  demais documentos para o dono (por `person_id`) ou gestão. São identificadores
  distintos — tratá-los como um só era origem de falha de acesso no monolito.

## Integração

**Recebe do core** (síncrono): vínculo responsável-aluno, via
`GET /api/v1/internal/guardianship/{studentId}/{guardianPersonId}`. Se o core
estiver fora, o acesso é **negado** — em verificação de permissão o padrão seguro
é negar.

**Envia ao core** (assíncrono, exchange `cadi.events`):

| Evento | Efeito no core |
|---|---|
| `AtestadoApproved` | marca as faltas do período como `JUSTIFIED` |
| `AtestadoRevoked` | reverte para `ABSENT` (rejeição, exclusão ou reaprovação) |

**Consultado pelo core** (síncrono): `/internal/covering` ao registrar uma falta.
Se este serviço estiver fora, o core grava `ABSENT` e o evento corrige depois —
degradação graciosa em vez de erro.

## Rodar

Sobe junto com a stack:

```bash
cd ../../deploy && docker compose up -d
```

Local (fora do container), com a infra já de pé:

```bash
pip install -r requirements.txt
alembic upgrade head
uvicorn app.main:app --reload --port 8000
```

Docs em `http://localhost:8000/docs`; health em `/actuator/health` (mesmo caminho
do core, para o compose e o Prometheus tratarem a stack de forma uniforme).

## Pendência conhecida

A validação do JWT ainda é **HS256 com segredo compartilhado** com o core — o que
significa que este serviço também consegue *emitir* tokens válidos. O Keycloak já
está na stack para migrar para **RS256 + JWKS**, onde os serviços recebem apenas a
chave pública. Ver `app/security.py`.
