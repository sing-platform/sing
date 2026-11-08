# Migração de dados legados: mural, notificações e financeiro

Copia dados das tabelas do PostgreSQL do core para os bancos novos. As tabelas antigas
**não são apagadas** nem alteradas: a exportação é somente leitura.

| Origem (PostgreSQL do core) | Destino |
|---|---|
| `mural_post`, `mural_post_audience`, `mural_post_audience_schoolroom`, `mural_post_view` | MongoDB do communication: `mural_posts`, `mural_views` |
| `notification` | MongoDB do communication: `notificacoes_usuario` |
| `sponsor`, `fiscal_document`, `financial_entry`, `financial_transaction` | MySQL `sing_finance` (serviço .NET) |
| `cobranca` | não migra (tabela só existe no destino) |

Nada aqui foi executado contra banco algum. Os scripts foram revisados com `bash -n` e
`node --check`; a lógica de `carregar/validar/reverter-mongo.js` foi exercitada com um
driver falso em memória. `psql`, `mongosh` e MySQL reais ainda não foram testados: ensaiar
em cópia dos bancos antes de qualquer execução (ver "Requer autorização para executar").

## Pré-requisitos

- Clientes no PATH: `psql`, `mongosh`, `mysql`; `bash` 4+ (usa `declare -A`).
- MySQL 8.0.31+ (a validação usa `EXCEPT`; a imagem `mysql:8.0` do deploy atende).
- Serviço `finance` já subiu uma vez e aplicou a migration `InitialCreate` (tabelas
  existem, vazias ou sem ids na faixa migrada). Para o Mongo, nada a criar: os índices
  são criados pelo communication (e a carga cria o único de `mural_views` se faltar).
- Usuário PostgreSQL só com leitura. O script força `default_transaction_read_only=on`.
- Credenciais por ambiente, nunca por argumento: `PGPASSWORD`/`PGPASSFILE`, `MONGO_URI`
  (inclui banco e senha), `MYSQL_PWD`. Os scripts não exibem esses valores.
- `SOURCE_TZ` definido (ver "Fuso"). Sem ele a exportação não roda.
- Os arquivos em `scripts/migracao-dados/saida/` contêm nome e e-mail de usuários e
  conteúdo do mural; ficam fora do git (`.gitignore`) e devem ser apagados depois.

## Ordem de execução

Todos os comandos rodam em `scripts/migracao-dados/`.

1. Parar escrita no legado: core fora do ar ou ao menos sem mural, notificações e
   financeiro (ver "Janela recomendada"). Manter os serviços novos **sem tráfego** e o
   `finance` com os jobs fora do ar (ver "Riscos").
2. `SOURCE_TZ=America/Sao_Paulo PGHOST=... PGDATABASE=... ./exportar-postgres.sh`
   Gera `saida/*.ndjson`, `saida/finance.sql`, `saida/resumo-mongo.txt`,
   `saida/resumo-mysql.txt`. Somente leitura.
3. Dry-run: `./carregar-mongo.sh --dry-run` e `./carregar-mysql.sh --dry-run`.
   Contam e detectam conflitos de id; não gravam.
4. Carga: `./carregar-mongo.sh` e `./carregar-mysql.sh`.
5. Validação: `./validar-mongo.sh` e `./validar-mysql.sh` (saem com código 1 se divergir).
6. Só com as duas validações em OK: subir os serviços novos com tráfego e os jobs do
   `finance`.
7. Rollback, se necessário: `CONFIRMAR=sim ./reverter-mongo.sh` e
   `CONFIRMAR=sim ./reverter-mysql.sh` (ambos aceitam `--dry-run`).

Arquivos: `exportar-postgres.sh`; `carregar-mongo.sh/.js`, `validar-mongo.sh/.js`,
`reverter-mongo.sh/.js`; `carregar-mysql.sh`, `validar-mysql.sh`, `reverter-mysql.sh`,
`comum-mysql.sh` (funções compartilhadas). `-h` em cada script mostra as variáveis.

## Mapeamento

IDs originais são preservados em todas as tabelas.

### mural_post → `mural_posts`

O communication usa o id numérico como `_id` do documento (não há campo `id`), então a
carga grava `_id = mural_post.id`.

| Origem | Destino | Observação |
|---|---|---|
| `mural_post.id` | `_id` | numérico; `counters.mural_posts.seq` = max(id) |
| `title`, `content` | `title`, `content` | |
| `author_id` | `authorUserId` | |
| `person.name` (por `person.user_id = users.id`), senão `users.username` | `authorName` | `users` **não tem coluna `name`**; o fallback é `username` |
| `users.email` | `authorEmail` | |
| `created_at`, `updated_at` | `createdAt`, `updatedAt` | `Date` em UTC; `updatedAt` pode ser `null` |
| `mural_post_audience.role_name` (`is_active`) | `audienceRoles[]` | ordenado; só linhas ativas |
| `mural_post_audience_schoolroom.schoolroom_id` (`is_active`) | `audienceSchoolroomIds[]` | só linhas ativas |
| `is_active` | `active` | posts inativos também migram |
| `created_by`, `deactivated_by`, `deactivated_at` | descartados | |

### mural_post_view → `mural_views`

| Origem | Destino |
|---|---|
| `post_id`, `user_id` | `postId`, `userId` (chave única `(postId, userId)`) |
| `viewed_at` | `viewedAt` (`null` = não lido) |
| `id`, `is_active`, auditoria | descartados |

### notification → `notificacoes_usuario`

Só migram linhas com `is_active = true` **e** `user_id` não nulo.

| Origem | Destino |
|---|---|
| `id` | `_id`; `counters.notificacoes_usuario.seq` = max(id migrado) |
| `user_id`, `title`, `message`, `type` | `userId`, `title`, `message`, `type` |
| `action_url`, `reference_key` | `actionUrl`, `referenceKey` (ficam `null` se nulos na origem) |
| `read_at`, `created_at` | `readAt` (`null` = não lida), `createdAt` |

### MySQL (nome de coluna igual à origem)

| Tabela | Conversões |
|---|---|
| `sponsor` | `student_id` sem FK (ver abaixo); `amount` DECIMAL(12,2) igual; `start_date`, `last_generated_date` DATE iguais |
| `fiscal_document` | `issue_date` DATE; `total_amount` DECIMAL(12,2) |
| `financial_entry` | `sponsor_id`, `fiscal_document_id` preservados e com FK no destino; `due_date`, `paid_at` DATE |
| `financial_transaction` | `datetime` → `datetime(6)`; `value_transaction` REAL → `float` |
| todas | `created_at`, `deactivated_at` TIMESTAMP(3) → `datetime(6)`; `is_active` BOOLEAN → `tinyint(1)`; `created_by`, `deactivated_by` bigint sem FK |

Colunas de enum (`direction`, `status`, `frequency`, `document_type`, `type`) são texto nos
dois lados e os valores coincidem com os enums do .NET (RECEIVABLE/PAYABLE;
PENDING/PAID/OVERDUE/CANCELED; MONTHLY…ANNUAL; NFE/NFSE/RECEIPT/INVOICE/OTHER;
INCOME/EXPENSE/REFUND).

Carga MySQL: `finance.sql` carrega tabelas temporárias (`CREATE TEMPORARY TABLE … LIKE`),
e uma transação única faz `INSERT … SELECT` na ordem sponsor, fiscal_document,
financial_entry, financial_transaction. Qualquer erro (inclusive id duplicado) desfaz tudo.
Depois do commit, `ALTER TABLE … AUTO_INCREMENT = max(id)+1` (DDL; não é transacional).
A carga aborta antes de gravar se houver qualquer id do destino dentro de `[min, max]`
da origem, ou se já existir `manifesto-mysql.env` de carga não revertida.

## O que não casa

- **FKs perdidas.** No destino não existem como FK: `sponsor.student_id` (aluno é do
  core), `financial_transaction.user_id`, `created_by`, `deactivated_by` (usuários são do
  core), `mural_post.author_id`, `mural_views.userId`, `audienceSchoolroomIds`. Os ids
  continuam válidos, mas ninguém garante integridade referencial entre bancos. Só
  `financial_entry → sponsor` e `financial_entry → fiscal_document` são FK reais no MySQL.
- **Fuso.** O Java gravou `TIMESTAMP` sem fuso com a hora local do servidor; o .NET grava
  UTC e o contrato do communication entrega UTC. Por isso a exportação converte com
  `(coluna AT TIME ZONE SOURCE_TZ) AT TIME ZONE 'UTC'`. É preciso saber o fuso real do
  JVM que gravou (`SOURCE_TZ`); se o core rodou em contêiner sem `TZ`, é `UTC`. Horários
  ambíguos ou inexistentes na virada de horário de verão são resolvidos pelo PostgreSQL
  sem aviso. `DATE` (due_date, paid_at, issue_date, start_date, last_generated_date) não
  é convertido. `viewed_at` e `datetime` seguem a mesma regra.
- **REAL → float.** `value_transaction` é `real` (4 bytes) no PostgreSQL e `float` (4
  bytes) no MySQL: mesma precisão, sem perda. É texto de ida e volta (`12345.67` →
  `12345.67`), mas a soma é comparada com arredondamento a centavos.
- **Índices.** Os índices do PostgreSQL (incluindo `uq_mural_view`) não são copiados; o
  destino usa os próprios (EF no MySQL, `createIndex` do communication no Mongo).
- **Descartes no Mongo.** Notificações com `is_active = false` ou `user_id` nulo (legadas,
  anteriores à V10) não migram. Linhas inativas de audiência (`mural_post_audience*`) não
  entram nos arrays. Campos de auditoria do core não existem no Mongo.
- **`referenceKey` nulo.** O tipo TypeScript do communication declara `referenceKey` como
  `string`; notificações antigas sem chave migram com `null`. Conferir se a deduplicação
  por `referenceKey` do communication tolera `null` antes de reabrir o tráfego.
- **Datas no Mongo.** Gravadas como `Date` (UTC), conforme o contrato.

## Riscos

- **Lançamentos PENDING vencidos viram OVERDUE e disparam evento.** O `OverduePaymentJob`
  do .NET roda a cada hora (primeira execução 1 h depois de subir) e marca todo
  `financial_entry` ativo com `status = PENDING` e `due_date` anterior a hoje (UTC) como
  `OVERDUE`, gravando um `payment.overdue` na outbox por lançamento. Migrar o legado com
  vencidos antigos gera uma rajada de eventos no primeiro ciclo. Mitigação: antes de subir
  o `finance`, contar `SELECT COUNT(*) FROM financial_entry WHERE is_active=1 AND
  status='PENDING' AND due_date < CURDATE()`; decidir com o negócio se isso é aceitável
  ou se os consumidores do evento devem estar prontos. Não há flag para desligar o job no
  código atual.
- **Duplicidade de parcelas de padrinho.** O `SponsorBillingJob` (diário, 01:00 UTC) parte
  de `last_generated_date` (ou de `start_date` menos um mês) e gera a parcela seguinte
  se vencer até o fim do mês. A carga copia `last_generated_date`, então não regenera o
  que já existia; ainda assim ele também checa `ExistsSponsorDueDate`. Se a carga for
  feita sem esse campo, o job geraria parcelas retroativas. Validar sponsors antes de
  liberar o job.
- **Validação só vale antes do tráfego.** Depois que os serviços novos recebem escrita,
  `viewedAt`, `readAt`, `status` etc. mudam e os totais deixam de bater com o arquivo de
  origem. Rode a validação logo após a carga.
- **Rollback depois de uso.** O rollback apaga por faixa de ids: perde o que usuários
  fizeram nas linhas migradas (leituras, edições) e as `mural_views` novas dos posts
  migrados. O contador do Mongo só volta ao valor anterior se não mudou desde a carga;
  o `AUTO_INCREMENT` do MySQL não é reduzido.
- **Posts com id já usado.** Se o communication já criou posts/notificações antes da
  migração, os ids colidem e a carga Mongo aborta (sem manifesto anterior). Resolver
  esvaziando o destino conscientemente, não forçando a carga.
- **Reexecução do Mongo.** Segura: usa `$setOnInsert`, nunca sobrescreve. Isso também
  significa que correções feitas na origem depois da primeira carga não propagam; para
  repropagar, reverter e recarregar.
- **Dados pessoais em disco** em `saida/` (e-mail, nomes, conteúdo): apagar ao final.

## Janela recomendada

Janela curta, fora do horário escolar, na ordem:

1. Parar escrita no legado (core em manutenção, ou rota de mural/notificações/financeiro
   bloqueada no gateway). Sem isso, o que for escrito depois da exportação se perde.
2. Exportar, carregar, validar (passos 2 a 5).
3. Só então virar o tráfego e iniciar os jobs do `finance`.

Ensaiar o procedimento completo antes numa cópia do PostgreSQL e em bancos novos vazios.

## Validação

`validar-mongo.sh` compara com `saida/resumo-mongo.txt` (gerado do PostgreSQL):
totais de posts, posts ativos, views, lidas e notificações (lidas e por tipo); views e
lidas **por post**; maiores ids; contadores cobrindo o maior id; amostra de ~20 documentos
por coleção com comparação campo a campo.

`validar-mysql.sh` compara com `saida/resumo-mysql.txt`: contagem por tabela, quantidade
e soma de `amount` por `direction`/`status`, soma de `sponsor` por frequência, total de
`fiscal_document`, soma de `value_transaction` por tipo, e diferença linha a linha (todas
as colunas) entre `finance.sql` e o destino, tabela por tabela.

Ambos imprimem as diferenças e saem com código 1 se qualquer verificação divergir. Só
olham as faixas de ids do manifesto.

## Rollback

- Manifestos gerados na carga em `saida/`: `manifesto-mongo.json` (faixas de `_id` de
  posts e notificações, faixa de `postId` das views, contadores antes e depois) e
  `manifesto-mysql.env` (min, max e total por tabela).
- `reverter-mongo.sh`: apaga `mural_views` por `postId`, `mural_posts` e
  `notificacoes_usuario` por `_id` dentro das faixas; devolve os contadores ao valor
  anterior (ou remove o contador, se não existia) somente se ainda estiverem no valor
  aplicado.
- `reverter-mysql.sh`: `DELETE` por faixa de id numa transação, na ordem
  financial_entry, fiscal_document, sponsor, financial_transaction.
- Nunca há `DROP`, `TRUNCATE` ou remoção de coleção/banco. Ambos exigem `CONFIRMAR=sim`
  (exceto com `--dry-run`) e renomeiam o manifesto para `.revertido-<data>` ao concluir.
- O legado não foi tocado, então o rollback definitivo é voltar o core a atender
  mural/notificações/financeiro.

## Requer autorização para executar

Cada item escreve em banco ou depende de parar o legado. Nada abaixo foi executado.

| Passo | Escreve em | Observação |
|---|---|---|
| `carregar-mongo.sh` (sem `--dry-run`) | MongoDB do communication: `mural_posts`, `mural_views`, `notificacoes_usuario`, `counters`; cria índice único de `mural_views` | grava `manifesto-mongo.json` local |
| `carregar-mysql.sh` (sem `--dry-run`) | MySQL `sing_finance`: `sponsor`, `fiscal_document`, `financial_entry`, `financial_transaction`; DDL `ALTER TABLE … AUTO_INCREMENT` | grava `manifesto-mysql.env` local |
| `reverter-mongo.sh` com `CONFIRMAR=sim` | MongoDB: apaga documentos e ajusta `counters` | |
| `reverter-mysql.sh` com `CONFIRMAR=sim` | MySQL: `DELETE` nas quatro tabelas | |
| Parar escrita no core / bloquear rotas no gateway | infraestrutura | afeta usuários |
| Iniciar o `finance` com jobs ativos após a carga | MySQL e outbox: `OverduePaymentJob` e `SponsorBillingJob` escrevem sozinhos | publica `payment.overdue` |

Não gravam em nenhum banco, mas leem dados de produção e criam arquivos locais com dados
pessoais: `exportar-postgres.sh` (somente leitura no PostgreSQL), `--dry-run` das cargas
(só leem o destino), `validar-mongo.sh`, `validar-mysql.sh` (leem o destino; `validar-mysql`
usa tabelas temporárias da própria sessão).
