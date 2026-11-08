# sing-communication

NestJS: notificação de `matricula.criada`, mural institucional e notificações in-app.

- Fila `communication.matricula.criada` na exchange `sing.events`
- Mongo próprio, database `sing_communication`
- Uma notificação por `eventId`
- `GET /api/v1/comunicacao/notificacoes/{eventId}` devolve a notificação ou 404

O mural (`/api/v1/mural`) e as notificações in-app (`/api/v1/notifications`) exigem o JWT do core.
A notificação da matrícula continua pública. Contrato em [`contracts/mural-notificacoes.md`](../../contracts/mural-notificacoes.md).

Variáveis: `JWT_SECRET` (mesmo valor do core; vazio recusa todo token), `CORE_BASE_URL` (padrão `http://localhost:8080`),
`MONGO_URI`, `RABBITMQ_*`.

## Teste

```bash
npm install
npm test
```

## Local

Com RabbitMQ e o Mongo deste serviço no ar:

```bash
npm run build
MONGO_URI='mongodb://communication:communication@localhost:27018/sing_communication?authSource=admin' \
RABBITMQ_HOST=localhost RABBITMQ_USER=sing RABBITMQ_PASSWORD=sing \
npm start
```

O contrato está em [`contracts/n2-matricula.md`](../../contracts/n2-matricula.md).
