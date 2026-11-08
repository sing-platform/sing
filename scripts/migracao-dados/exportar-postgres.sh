#!/usr/bin/env bash
set -euo pipefail

uso() {
  cat <<'AJUDA'
Exporta, somente leitura, os dados legados do PostgreSQL do core.

Variaveis de ambiente:
  PGHOST PGPORT PGUSER PGDATABASE   conexao (PGPASSWORD ou PGPASSFILE para a senha)
  SOURCE_TZ                         obrigatoria; fuso em que o Java gravou os TIMESTAMP
                                    (ex.: America/Sao_Paulo ou UTC)
  DIR_DADOS                         destino dos arquivos (padrao: ./saida ao lado do script)

Saidas em DIR_DADOS:
  mural_posts.ndjson  mural_views.ndjson  notificacoes_usuario.ndjson   (Mongo)
  finance.sql                                                          (MySQL)
  resumo-mongo.txt  resumo-mysql.txt                                   (validacao)
Todos os horarios saem convertidos para UTC.
AJUDA
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  uso
  exit 0
fi

: "${PGDATABASE:?defina PGDATABASE}"
: "${SOURCE_TZ:?defina SOURCE_TZ (ex.: America/Sao_Paulo ou UTC)}"
[[ "$SOURCE_TZ" =~ ^[A-Za-z0-9_/+-]+$ ]] || { echo "SOURCE_TZ invalido" >&2; exit 2; }

DIR_DADOS="${DIR_DADOS:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/saida}"
mkdir -p "$DIR_DADOS"
DIR_DADOS="$(cd "$DIR_DADOS" && pwd)"
[[ "$DIR_DADOS" != *"'"* ]] || { echo "DIR_DADOS nao pode conter aspas simples" >&2; exit 2; }

export PGCLIENTENCODING=UTF8
export PGOPTIONS="-c default_transaction_read_only=on"

ts() {
  printf "to_char((%s AT TIME ZONE '%s') AT TIME ZONE 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"')" "$1" "$SOURCE_TZ"
}

tsmy() {
  printf "to_char((%s AT TIME ZONE '%s') AT TIME ZONE 'UTC','YYYY-MM-DD HH24:MI:SS.US')" "$1" "$SOURCE_TZ"
}

lit() {
  printf "coalesce('''' || replace(replace((%s)::text, E'\\\\\\\\', E'\\\\\\\\\\\\\\\\'), '''', '''''') || '''', 'NULL')" "$1"
}

num() {
  printf "coalesce((%s)::text, 'NULL')" "$1"
}

bool() {
  printf "case when %s then '1' else '0' end" "$1"
}

audit_cols() {
  local p="$1"
  printf "%s || ',' || %s || ',' || %s || ',' || %s || ',' || %s" \
    "$(lit "$(tsmy "$p.created_at")")" \
    "$(num "$p.created_by")" \
    "$(bool "$p.is_active")" \
    "$(num "$p.deactivated_by")" \
    "$(lit "$(tsmy "$p.deactivated_at")")"
}

psql -X -q -v ON_ERROR_STOP=1 -A -t <<SQL
\o '$DIR_DADOS/mural_posts.ndjson'
select json_build_object(
  'id', p.id,
  'title', p.title,
  'content', p.content,
  'authorUserId', p.author_id,
  'authorName', coalesce((select pe.name from person pe where pe.user_id = u.id order by pe.id limit 1), u.username),
  'authorEmail', u.email,
  'createdAt', $(ts p.created_at),
  'updatedAt', $(ts p.updated_at),
  'audienceRoles', coalesce((select json_agg(a.role_name order by a.role_name) from mural_post_audience a where a.post_id = p.id and a.is_active), '[]'::json),
  'audienceSchoolroomIds', coalesce((select json_agg(s.schoolroom_id order by s.schoolroom_id) from mural_post_audience_schoolroom s where s.post_id = p.id and s.is_active), '[]'::json),
  'active', p.is_active
)
from mural_post p join users u on u.id = p.author_id
order by p.id;

\o '$DIR_DADOS/mural_views.ndjson'
select json_build_object('postId', v.post_id, 'userId', v.user_id, 'viewedAt', $(ts v.viewed_at))
from mural_post_view v
order by v.post_id, v.user_id;

\o '$DIR_DADOS/notificacoes_usuario.ndjson'
select json_build_object(
  'id', n.id, 'userId', n.user_id, 'title', n.title, 'message', n.message, 'type', n.type,
  'actionUrl', n.action_url, 'referenceKey', n.reference_key,
  'readAt', $(ts n.read_at), 'createdAt', $(ts n.created_at)
)
from notification n
where n.is_active and n.user_id is not null
order by n.id;

\o '$DIR_DADOS/finance.sql'
select 'SET NAMES utf8mb4;';
select 'INSERT INTO stg_sponsor VALUES (' || $(num s.id) || ',' || $(num s.student_id) || ',' || $(lit s.name) || ',' || $(lit s.email) || ',' || $(lit s.phone) || ',' || $(num s.amount) || ',' || $(lit s.frequency) || ',' || $(lit s.start_date) || ',' || $(lit s.last_generated_date) || ',' || $(audit_cols s) || ');'
from sponsor s order by s.id;
select 'INSERT INTO stg_fiscal_document VALUES (' || $(num d.id) || ',' || $(lit d.document_type) || ',' || $(lit d.document_number) || ',' || $(lit d.issue_date) || ',' || $(num d.total_amount) || ',' || $(lit d.supplier_name) || ',' || $(lit d.supplier_doc) || ',' || $(lit d.file_url) || ',' || $(audit_cols d) || ');'
from fiscal_document d order by d.id;
select 'INSERT INTO stg_financial_entry VALUES (' || $(num e.id) || ',' || $(lit e.direction) || ',' || $(lit e.status) || ',' || $(lit e.due_date) || ',' || $(lit e.paid_at) || ',' || $(num e.amount) || ',' || $(lit e.category) || ',' || $(lit e.description) || ',' || $(num e.sponsor_id) || ',' || $(num e.fiscal_document_id) || ',' || $(audit_cols e) || ');'
from financial_entry e order by e.id;
select 'INSERT INTO stg_financial_transaction VALUES (' || $(num t.id) || ',' || $(lit t.type) || ',' || $(lit "$(tsmy t.datetime)") || ',' || $(num t.value_transaction) || ',' || $(lit t.description) || ',' || $(lit t.receipt_url) || ',' || $(num t.user_id) || ',' || $(audit_cols t) || ');'
from financial_transaction t order by t.id;

\o '$DIR_DADOS/resumo-mongo.txt'
select 'mural_posts.total|' || count(*) from mural_post;
select 'mural_posts.ativos|' || count(*) from mural_post where is_active;
select 'mural_posts.max_id|' || coalesce(max(id), 0) from mural_post;
select 'mural_views.total|' || count(*) from mural_post_view;
select 'mural_views.lidas|' || count(*) from mural_post_view where viewed_at is not null;
select 'mural_post.' || p.id || '.views|' || count(v.id) from mural_post p left join mural_post_view v on v.post_id = p.id group by p.id;
select 'mural_post.' || p.id || '.lidas|' || count(v.viewed_at) from mural_post p left join mural_post_view v on v.post_id = p.id group by p.id;
select 'notificacoes_usuario.total|' || count(*) from notification where is_active and user_id is not null;
select 'notificacoes_usuario.lidas|' || count(*) from notification where is_active and user_id is not null and read_at is not null;
select 'notificacoes_usuario.max_id|' || coalesce(max(id), 0) from notification where is_active and user_id is not null;
select 'notificacoes_usuario.tipo.' || type || '|' || count(*) from notification where is_active and user_id is not null group by type;

\o '$DIR_DADOS/resumo-mysql.txt'
select 'sponsor.total|' || count(*) from sponsor;
select 'sponsor.valor.' || frequency || '|' || sum(amount) from sponsor group by frequency;
select 'fiscal_document.total|' || count(*) from fiscal_document;
select 'fiscal_document.valor|' || coalesce(sum(total_amount), 0.00) from fiscal_document;
select 'financial_entry.total|' || count(*) from financial_entry;
select 'financial_entry.qtd.' || direction || '.' || status || '|' || count(*) from financial_entry group by direction, status;
select 'financial_entry.valor.' || direction || '.' || status || '|' || sum(amount) from financial_entry group by direction, status;
select 'financial_transaction.total|' || count(*) from financial_transaction;
select 'financial_transaction.valor.' || type || '|' || round(sum(round(value_transaction::double precision::numeric, 2)), 2) from financial_transaction group by type;
\o
SQL

for arq in resumo-mongo.txt resumo-mysql.txt; do
  LC_ALL=C sort -o "$DIR_DADOS/$arq" "$DIR_DADOS/$arq"
done

echo "exportacao concluida em $DIR_DADOS"
wc -l "$DIR_DADOS"/*.ndjson "$DIR_DADOS/finance.sql"
