# Contrato: mural e notificações no communication

O mural e as notificações in-app saem do core e passam a morar no `communication`
(NestJS + MongoDB). O core continua dono de usuários, pessoas, turmas e matrículas.
Nenhum serviço lê o banco do outro: o que o communication precisa de dado acadêmico
chega por REST (com o token do usuário) ou por evento.

## Identidade

O communication valida o JWT emitido pelo core com o mesmo `JWT_SECRET`.

| Claim | Uso |
|---|---|
| `sub` | e-mail do usuário |
| `role` | string única com prefixo, por exemplo `ROLE_ADMIN` |
| `uid` | id do usuário (`users.id`); é a identidade usada no mural e nas notificações |
| `pid` | id da pessoa; opcional |

A chave é o segredo decodificado em Base64 estrito; se não for Base64 válido, os bytes
UTF-8 do texto. O algoritmo aceito é HMAC (HS256, HS384 ou HS512): o jjwt do core escolhe
pelo tamanho da chave. Token ausente, inválido ou expirado: 401. Papel sem permissão: 403.

## Rotas públicas preservadas

Os caminhos não mudam. O gateway (Traefik e Ingress) passa a mandar estes prefixos ao
communication:

| Método | Caminho | Papéis |
|---|---|---|
| GET | `/api/v1/mural` | qualquer autenticado |
| GET | `/api/v1/mural/{id}` | qualquer autenticado (403 se não for destinatário) |
| GET | `/api/v1/mural/unread-count` | qualquer autenticado |
| POST | `/api/v1/mural/mark-all-read` | qualquer autenticado |
| POST | `/api/v1/mural` | PROFESSOR, SECRETARY, ADMIN |
| PUT | `/api/v1/mural/{id}` | PROFESSOR, SECRETARY, ADMIN |
| DELETE | `/api/v1/mural/{id}` | PROFESSOR, SECRETARY, ADMIN |
| GET | `/api/v1/mural/engajamento?start=yyyy-MM-dd&end=yyyy-MM-dd` | ADMIN, SECRETARY, MANAGER |
| GET | `/api/v1/notifications` | SECRETARY, ADMIN, MANAGER |
| PATCH | `/api/v1/notifications/{id}/read` | SECRETARY, ADMIN, MANAGER |
| PATCH | `/api/v1/notifications/read-all` | SECRETARY, ADMIN, MANAGER |

Corpos, status HTTP, mensagens e regras são os do core até esta mudança. ADMIN e SECRETARY
são gestores do mural: veem todos os posts ativos e editam qualquer post. Os IDs continuam
numéricos e sequenciais. Datas saem como `LocalDateTime` ISO sem fuso, em UTC.

`/api/v1/mural/engajamento` devolve uma linha por post ativo criado no período, em ordem
de criação decrescente: `postId`, `title`, `createdAt`, `authorUsername`, `viewCount`,
`readCount`. O relatório `engajamento-mural` do core consome esta rota.

## Destinatários do mural (core)

`POST /api/v1/internal/mural/destinatarios`, no core, com o token do autor.
Papéis: PROFESSOR, SECRETARY, ADMIN.

```json
{ "audienceRoles": ["ROLE_STUDENT"], "audienceSchoolroomIds": [3] }
```

O core aplica as regras de público de antes: professor só publica para as próprias
turmas e não usa papel; gestor precisa de papel ou turma. Erros: 400 e 403 com as mesmas
mensagens de antes.

```json
{
  "author": { "userId": 1, "name": "Ana Souza", "email": "admin@sing.local", "role": "ROLE_ADMIN" },
  "recipients": [ { "userId": 7, "email": "aluno@sing.local" } ]
}
```

`recipients` já exclui o autor. Como antes, o público por turma traz só alunos com usuário
habilitado; o público por papel traz todos os usuários do papel. O communication chama esta
rota ao criar e ao editar um post; na edição, só quem ainda não tinha visualização ganha
visualização e e-mail.

## E-mail do mural

O communication publica na exchange `sing.notifications`, routing key `email.send`, com
`content_type: application/json` e mensagem persistente:

```json
{ "to": "aluno@sing.local", "subject": "Nova publicação no mural: <título>", "body": "<html>", "html": true }
```

A entrega SMTP continua no consumidor `email.queue` do core, que também atende a
convocação de pré-cadastro. Se o broker falhar, a mensagem fica na outbox do Mongo do
communication e é reenviada.

## Notificação de vaga na lista de espera (core → communication)

O core publica na exchange `sing.events`, routing key `notificacao.solicitada`. A fila
`communication.notificacao.solicitada` é durável e ligada a essa routing key; core e
communication a declaram.

```json
{
  "eventId": "6f1c2a4e-1b2c-4d5e-8f90-123456789abc",
  "eventType": "notificacao.solicitada",
  "version": 1,
  "occurredAt": "2026-11-08T17:00:00Z",
  "payload": {
    "type": "WAITING_LIST_VACANCY",
    "title": "Vaga disponível na lista de espera",
    "message": "Há 1 vaga(s) na oficina \"Violão\" (turno manhã) e 2 candidato(s) aguardando.",
    "actionUrl": "/dashboard/secretaria/espera",
    "referenceKey": "WAITING_LIST:4:MORNING",
    "recipientUserIds": [1, 2, 5]
  }
}
```

O core resolve os destinatários (usuários ativos de SECRETARY, ADMIN e MANAGER). O
communication grava uma notificação por destinatário, exceto quando já existe uma não
lida com o mesmo `referenceKey` para ele. Reentrega do mesmo `eventId` não duplica.

## Fora deste contrato

`matricula.criada`, `communication.matricula.criada` e
`GET /api/v1/comunicacao/notificacoes/{eventId}` seguem o contrato N2
(`contracts/n2-matricula.md`).
