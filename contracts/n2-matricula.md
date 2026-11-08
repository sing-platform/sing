# Contrato N2 — matrícula criada e isolamento do documents

Este arquivo é o único contrato fechado deste recorte. Implementação, deploy e mudança de runtime ficam fora dele. Nada aqui passa a funcionar só porque está escrito.

Três leituras, sem misturar:

| Leitura | O que é |
|---|---|
| Estado atual | O que a `main` faz hoje. |
| PR 2 | [feat(core): publica matricula.criada no RabbitMQ](https://github.com/sing-platform/sing/pull/2), aberto. Referência de um publisher flat. Não é este contrato e não entra por este card. |
| Contrato alvo | O combinado do SIN-5. Ainda não existe no código. Onde um caminho HTTP é novo, está marcado como **proposto**. |

## Fora deste contrato

Whitelabel, multitenancy, analytics, frontend, backlog 57, pagamento real, e-mail real, AWS, deploy e qualquer código deste card. O plano em `docs/docs/plano-whitelabel-gestao-escolar.md` não redefine este evento. Relatórios, identidade e a fila de espera também não.

## Domínio que o evento usa

No core, os identificadores de matrícula, aluno, turma e oficina são `Long` gerados por identidade. No JSON vão como número, nunca como string.

Não há entidade curso. A matrícula (`enrollment`) liga um aluno (`student`, nome em `person.name`) a uma turma (`schoolroom`). A turma aponta para uma oficina (`workshop`). Os nomes do payload são os dessas colunas: `student`/`person.name`, `schoolroom.name`, `workshop.name`.

Uma requisição cria uma matrícula por turma aceita, no máximo duas. Se a matrícula nova desativa outra da mesma oficina (transferência que o `EnrollmentService` já faz), o evento e a cobrança saem só da matrícula criada. A desativada não vira `matricula.criada`.

## Datas

Dois formatos, sem mistura.

| Campo | Formato | Significado |
|---|---|---|
| `occurredAt` | `YYYY-MM-DDTHH:mm:ssZ` | Instante UTC. Segundos, sufixo `Z`, sem fração e sem offset local. |
| `payload.data` | `YYYY-MM-DD` | Data de calendário da matrícula (`enrollment.enrollmentDate`, `LocalDate`). Sem hora. |
| `vencimento` | `YYYY-MM-DD` | Data de calendário da cobrança. Sem hora. |

Exemplo válido de instante: `2026-11-05T18:30:00Z`. Exemplo válido de data: `2026-11-05`. `2026-11-05T18:30:00-03:00` e `05/11/2026` não valem nestes campos.

## Envelope `matricula.criada`

Campos obrigatórios, nesta forma:

| Campo | Tipo | Regra |
|---|---|---|
| `eventId` | string UUID | Canônico, minúsculo, `8-4-4-4-12`. Nasce uma vez, na mesma transação da matrícula. Reentrega e retry republicam o mesmo valor. |
| `eventType` | string | Exatamente `matricula.criada`. |
| `version` | número | Inteiro `1`. |
| `occurredAt` | string | Instante no formato da tabela acima. |
| `payload.matriculaId` | número | `enrollment.id`. |
| `payload.data` | string | Data da matrícula. |
| `payload.aluno.id` | número | `student`/`person.id`. |
| `payload.aluno.nome` | string | Nome do aluno. |
| `payload.turma.id` | número | `schoolroom.id`. |
| `payload.turma.nome` | string | Nome da turma. |
| `payload.oficina.id` | número | `workshop.id`. |
| `payload.oficina.nome` | string | Nome da oficina. |

Não entram `aggregateId`, `tenantId`, `correlationId`, `curso`, `alunoId` solto nem `turmaId` solto. O envelope de atestado que o documents já publica (`eventId` novo a cada envio, `aggregateId` string) continua sendo o de atestado. Não é o modelo deste evento.

Exemplo válido. Pessoas, turmas e números são fictícios, não são semente do sistema:

```json
{
  "eventId": "11111111-1111-4111-8111-111111111111",
  "eventType": "matricula.criada",
  "version": 1,
  "occurredAt": "2026-11-05T18:30:00Z",
  "payload": {
    "matriculaId": 9001,
    "data": "2026-11-05",
    "aluno": { "id": 501, "nome": "Aluno Exemplo Fictício" },
    "turma": { "id": 77, "nome": "Turma Exemplo Manhã" },
    "oficina": { "id": 12, "nome": "Oficina Exemplo de Teatro" }
  }
}
```

Duas turmas na mesma requisição produzem dois envelopes, dois `eventId` e duas cobranças. O `eventId` de cada matrícula não muda se o broker ou o finance caírem e o core tentar de novo.

## RabbitMQ

A exchange topic `sing.events` já existe na `main` para `atestado.*` (fila `core.atestado.events`). Este evento usa a mesma exchange e a routing key `matricula.criada`.

Duas filas duráveis, cada uma com binding próprio nessa routing key:

| Fila | Quem consome |
|---|---|
| `documents.matricula.criada` | documents, só o recorte do comprovante |
| `communication.matricula.criada` | communication |

Cada fila recebe a sua cópia. Falha numa não segura a outra. Não há fila única chamada `matricula.criada`: esse nome é o da fila de inspeção do PR 2, não deste contrato.

A exchange `sing.notifications` (`email.send`, `notification.send`, `whatsapp.send`) permanece o canal de e-mail e aviso do core. `matricula.criada` não passa por ela.

Entrega é pelo menos uma vez. Cada consumidor deduplica de forma persistente pela `eventId` antes de gravar efeito. A segunda cópia não cria outro comprovante nem outra notificação. A deduplicação não fica só em memória.

## Finance — `POST /cobrancas`

O serviço finance é .NET com MySQL, idempotente por `matriculaId`, e é o único dono do financeiro: `/cobrancas` e os recursos `/api/v1/finance/entries`, `/api/v1/finance/sponsors` e `/api/v1/finance/fiscal-documents`. O core só chama o HTTP abaixo.

Contrato HTTP que o core chama depois do commit:

`POST /cobrancas`

```json
{
  "matriculaId": 9001,
  "valorCentavos": 15000,
  "vencimento": "2026-11-15"
}
```

`matriculaId` é número `Long`. `valorCentavos` é inteiro positivo (centavos). `vencimento` é data de calendário.

Resposta, no primeiro aceite:

```json
{ "id": 44001, "status": "pendente" }
```

`id` é o identificador numérico da cobrança. `status` neste contrato é só `pendente`. Não há pagamento, gateway, baixa nem boleto.

| Situação | Resultado |
|---|---|
| Primeira vez para aquele `matriculaId` | `201` e o corpo acima. |
| Repetição com o mesmo `matriculaId`, o mesmo `valorCentavos` e o mesmo `vencimento` | `200` e a cobrança já existente. Continua uma cobrança. |
| Mesmo `matriculaId` com valor ou vencimento diferente | `409`. Não grava outra cobrança e não substitui a anterior. |

Valor e prazo são fictícios e configuráveis, ainda sem chave no `application.yml`:

| Configuração proposta | Exemplo | Efeito |
|---|---|---|
| `N2_COBRANCA_VALOR_CENTAVOS` | `15000` | R$ 150,00 fictícios. Não é tabela de preço. |
| `N2_COBRANCA_PRAZO_DIAS` | `10` | `vencimento` = `payload.data` mais esses dias, em calendário. `2026-11-05` + 10 = `2026-11-15`. |

## Documents — recorte isolado, resto preservado

Estado atual, que este contrato não migra:

- FastAPI, banco PostgreSQL `sing_documents`, credencial própria no compose (`postgres-documents`).
- Tabela `user_documents` e Alembic `0001_initial_schema`.
- Arquivo no MinIO, bucket `sing-documents`, chave `documents/{ownerPersonId}/{uuid}{ext}`.
- Upload até 10 MB, MIME por magic bytes, fluxo de atestado.
- HTTP ao core só para o vínculo de responsável (`GET /api/v1/internal/guardianship/...`). Se o core não responde, o acesso é negado.
- Publica `AtestadoApproved` e `AtestadoRevoked` em `sing.events`. O `eventId` desse fluxo é gerado em cada publicação.

O comprovante da matrícula é outro recorte, no mesmo processo FastAPI, sem levar o resto para Mongo:

- Database MongoDB e usuário só deste recorte, distintos dos do communication. Nome de contrato: database `sing_documents_comprovantes`.
- Não usa a URL do PostgreSQL `sing_documents`, o bucket MinIO nem a connection string do communication.
- Guarda metadados e o arquivo pequeno no mesmo documento BSON, arquivo em Base64.
- O limite duro do MongoDB é 16 MiB por documento (`16 * 1024 * 1024` bytes). Base64 ocupa cerca de 4/3 do binário, mais preenchimento. A configuração proposta `DOCUMENTS_COMPROVANTE_MAX_BYTES` vale `1048576` (1 MiB cru). 1 MiB em Base64 fica perto de 1,4 MiB e cabe folgado com os metadados. Acima do teto configurado, ou se o BSON passasse de 16 MiB, o comprovante não é gravado.
- O comprovante é gerado só com o payload do evento (ids e nomes já vêm nele). Este recorte não chama o core por HTTP.
- Dedup persistente por `eventId`. Reentrega não gera segundo arquivo.
- Payload sem os campos obrigatórios não dispara consulta ao core. Fica um registro visível de rejeição, também único por `eventId`.

Campos do documento de comprovante: `eventId`, `matriculaId`, `data`, `aluno`, `turma`, `oficina`, `occurredAt`, `mimeType` (`application/pdf`), `tamanhoBytes` (tamanho cru), `arquivoBase64`, `nomeArquivo` no padrão `comprovante-matricula-9001.pdf`.

## Communication — outra fila, outro Mongo

Estado atual: comunicação ainda é pacote Java dentro do core. `GET /api/v1/notifications` lista aviso do usuário autenticado na tabela PostgreSQL `notification`. Mural em `/api/v1/mural`. E-mail segue `sing.notifications` / `email.send`, com outbox `message_outbox`. Não há serviço NestJS no repositório (entrega E3 do README ainda aberta). Esse `GET` não é a consulta deste contrato.

Contrato alvo:

- Processo NestJS, ainda não criado, consome somente `communication.matricula.criada`.
- MongoDB próprio e credencial própria. Nome de contrato: database `sing_communication`. Não é `sing_documents_comprovantes` nem `sing_documents`.
- Grava uma notificação por `eventId`. Reentrega não insere outra.
- Não envia e-mail, SMTP nem WhatsApp por causa deste evento.
- Consulta REST proposta, inexistente hoje: `GET /api/v1/comunicacao/notificacoes/{eventId}`. Devolve a notificação daquele evento ou `404`. Não substitui `GET /api/v1/notifications`.

## Falhas — matrícula firme, integrações depois

Na mesma transação PostgreSQL do core entram a matrícula e as pendências de publicação e de cobrança, já com o `eventId` definitivo. Se a transação desfaz, não há matrícula nem pendência.

Depois do commit, publicar no RabbitMQ e chamar `POST /cobrancas` são trabalhos independentes. Um falhar não desfaz a matrícula e não impede o outro.

O retry fica nas pendências do PostgreSQL do core. Intervalo e limite são configuráveis (ainda sem chave no YAML):

| Configuração proposta | Exemplo |
|---|---|
| `N2_RETRY_INTERVALO` | `30s` |
| `N2_RETRY_LIMITE` | `10` |
| `N2_CLAIM_SEGUNDOS` | `60` |

O core pode subir em 3 réplicas. Antes de trabalhar uma pendência, a réplica faz claim da linha (`claimed_by` e prazo). Só quem detém o claim publica ou cobra. O claim vence para outra réplica assumir se o processo morrer. O `eventId` da linha não é trocado.

Tentativa esgotada permanece visível, status `esgotada`, linha conservada. `409` do finance é terminal e visível (`conflito`), sem novo retry: repetir o corpo divergente não cria a cobrança certa. Queda de rede e `5xx` entram no retry. Reinício do core não apaga pendência, porque ela está no PostgreSQL e não na memória do processo.

O outbox atual (`message_outbox`, releitura a cada 60 s, sem claim e sem limite) segue só para `email.send`. Não é o mecanismo deste contrato.

## POST do core

Já existe `POST /api/v1/enrollments`, papéis `ADMIN` e `SECRETARY`. O corpo já é o deste contrato:

```json
{ "studentId": 501, "schoolRoomIds": [77] }
```

`studentId` obrigatório. `schoolRoomIds` de 1 a 2 ids, sem repetir. Com duas turmas, o serviço valida o lote antes de gravar. Respostas de erro já documentadas no controller: `400`, `401`, `403`, `404`, `422`.

O que existe hoje de sucesso é `201` com texto (`Aluno matriculado com sucesso!` ou `Matrículas concluídas com sucesso!`). Não devolve id nem pendência. Não há consulta de andamento.

Resposta de sucesso proposta, ainda não implementada, um item por matrícula criada:

```json
{
  "matriculas": [
    {
      "matriculaId": 9001,
      "eventId": "11111111-1111-4111-8111-111111111111",
      "publicacao": "pendente",
      "cobranca": "pendente"
    }
  ]
}
```

Consulta de andamento proposta, também inexistente: `GET /api/v1/enrollments/{matriculaId}/integracoes`.

```json
{
  "matriculaId": 9001,
  "eventId": "11111111-1111-4111-8111-111111111111",
  "publicacao": "pendente",
  "cobranca": "pendente",
  "tentativasPublicacao": 0,
  "tentativasCobranca": 0
}
```

`publicacao` e `cobranca` usam `pendente`, `concluida` ou `esgotada`. `cobranca` também pode ser `conflito`. Isto não é um endpoint já mapeado no `EnrollmentController`.

## Aceites de ponta a ponta, para um card futuro

Ainda não há teste destes cenários. Quando a implementação existir, os três precisam passar:

1. Finance fora do ar: a matrícula permanece. Quando o finance volta, a mesma pendência gera exatamente uma cobrança para aquele `matriculaId`.
2. O mesmo `eventId` entregue de novo não cria segundo comprovante nem segunda notificação.
3. Reiniciar o core não perde pendência de publicação nem de cobrança.

## O que o PR 2 faz e este contrato não adota

O PR 2 está aberto em `feat/core-rabbitmq`. O publisher de lá manda um mapa plano, sem envelope:

```json
{"matriculaId": 81, "alunoId": 25, "turmaId": 1, "data": "2026-11-04"}
```

Não há `eventId`, `eventType`, `version`, `occurredAt`, nomes, nem oficina. A fila declarada para ver a mensagem no painel se chama `matricula.criada`. Se o broker falha, o erro vai para o log e a matrícula segue, sem pendência persistida deste evento. Serve de referência do primeiro passo. O contrato alvo é o deste arquivo.
