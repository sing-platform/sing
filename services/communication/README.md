# sing-communication

Recorte NestJS da notificação de `matricula.criada`.

- Fila `communication.matricula.criada` na exchange `sing.events`
- Mongo próprio, database `sing_communication`
- Uma notificação por `eventId`
- `GET /api/v1/comunicacao/notificacoes/{eventId}` devolve a notificação ou 404

Não envia e-mail nem WhatsApp. O `GET /api/v1/notifications` do core continua no PostgreSQL.

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
