# sing-finance

Serviço financeiro: padrinhos, lançamentos, documentos fiscais e cobrança da matrícula (N2).
**C# / .NET 8 · MySQL próprio · RabbitMQ (outbox)**

Extraído do módulo `br.sing.finance` do core.

## Endpoints

| Prefixo | Auth | Papel |
|---|---|---|
| `POST /cobrancas` | não (rede interna) | Contrato N2 — core cria cobrança por `matriculaId` |
| `/api/v1/finance/sponsors` | JWT `ROLE_ADMIN` / `ROLE_MANAGER` | Padrinhos |
| `/api/v1/finance/entries` | JWT | Lançamentos + resumo + baixa |
| `/api/v1/finance/fiscal-documents` | JWT | Notas e recibos |
| `GET /actuator/health` | não | Ops |
| `/finance/swagger` | não | Swagger UI |

## Integração

- **Core (HTTP):** ao cadastrar padrinho, confirma o aluno em `GET /api/v1/students/{id}` com o mesmo Bearer. Se o core estiver fora, o cadastro é negado.
- **RabbitMQ:** lançamentos `PENDING` vencidos viram `OVERDUE` e publicam `PaymentOverdue` (`payment.overdue`) via outbox.
- **Job diário:** gera recebíveis de padrinho ativos (mesma regra do monolito).

## Rodar local

```bash
dotnet run --project Sing.Finance.csproj
```

Com a stack:

```bash
cd ../../deploy && docker compose up -d --build finance
```

Health: `http://localhost:8080/actuator/health` (via Traefik) ou porta direta `5080` fora do compose.

## Testes

```bash
dotnet test Sing.Finance.sln
```
