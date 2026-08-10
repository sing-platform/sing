# Backend SGS-CADI

## Banco de dados e Flyway após squash de migrations

As migrations incrementais antigas (V1–V7) foram arquivadas em `src/main/resources/db/migration_old/{h2,sqlserver}` apenas para referência. O Flyway usa somente `src/main/resources/db/migration/{h2,sqlserver}`.

Foi introduzido um **único script** `V1__criar_schema_inicial.sql` por dialeto (estado consolidado do schema). Quem já tinha aplicado as versões antigas precisa **recriar o banco** ou limpar o histórico Flyway antes de subir a aplicação:

- **H2 (arquivo, perfil `dev`)**: apague os arquivos do banco no caminho configurado em `application-dev.yml` (ex.: `./data/cadidb.*`) ou use outro nome de banco na URL JDBC.
- **SQL Server**: em ambiente de desenvolvimento aceitável, use `flyway clean` (destrutivo) ou recrie o schema/database e rode `migrate` de novo.

Sem esse reset, o Flyway pode falhar ou o Hibernate (`ddl-auto: validate`) pode divergir do schema existente.

## Tabelas `role` e `user`

As tabelas de perfil e conta foram renomeadas de `roles` / `users` para **`role`** e **`user`**. No **SQL Server** os nomes são criados como `[role]` e `[user]` (palavras reservadas). No **H2** (dev/test), a URL JDBC inclui `NON_KEYWORDS=USER,ROLE` para permitir esses identificadores sem aspas e manter o alinhamento com o Hibernate.

## Tabela `audit_log`

Há **triggers em todas as tabelas de negócio** (não em `audit_log`), que registram `INSERT` / `UPDATE` / `DELETE` com snapshot em `old_data` / `new_data` (JSON). No **SQL Server**, os `CREATE TRIGGER` vêm em batches separados por **`GO`**. O campo **`password`** da tabela **`user`** não entra no JSON de auditoria. **`changed_by`** segue `NULL` no banco (dá para evoluir com `SESSION_CONTEXT` ou camada de app).

**Subida do sistema:** ao iniciar, o **Flyway** executa o `V1__criar_schema_inicial.sql` do dialeto ativo (**H2** ou **SQL Server**) e **cria tabelas, índices e triggers** no banco. Não há passo em Python nem em Java para instalar esses triggers — só migration SQL.

Se mudarem o schema, editem (ou versionem com `V2__…`) os próprios `CREATE TRIGGER` no mesmo arquivo de migration, à mão, para ficarem alinhados às colunas.

**Java vs banco:** auditoria **só em Java** (listeners, Envers) facilita `changed_by` com o usuário logado, mas **não** registra alterações feitas direto no SQL. **Triggers no banco** fazem o contrário. Podem coexistir se precisarem dos dois perfis.

Auditoria adicional só na aplicação ainda pode fazer sentido para enriquecer o log com contexto da requisição.

## Relatórios (`/api/v1/reports`)

- **14 relatórios** (JSON + export CSV/XLSX), restritos a `ADMIN`, `SECRETARY`, `MANAGER`.
- **Produção (SQL Server):** 3 stored procedures em `V4__create_report_procedures.sql` (`sp_relatorio_frequencia_por_turma`, `sp_relatorio_alunos_em_risco`, `sp_relatorio_chamadas_pendentes`).
- **Dev/test (H2):** as mesmas 3 consultas rodam como SQL inline equivalente (H2 não executa T-SQL).
- Frontend: `/dashboard/relatorios` (secretaria e gestão).
