#!/usr/bin/env bash

: "${MYSQL_DATABASE:?defina MYSQL_DATABASE}"
: "${MYSQL_USER:?defina MYSQL_USER}"
: "${MYSQL_HOST:=127.0.0.1}"
: "${MYSQL_PORT:=3306}"

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_DADOS="${DIR_DADOS:-$AQUI/saida}"
MANIFESTO="$DIR_DADOS/manifesto-mysql.env"
TABELAS="sponsor fiscal_document financial_entry financial_transaction"

mysql_cli() {
  mysql --host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" \
    --default-character-set=utf8mb4 --batch --skip-column-names "$@"
}

faixa_origem() {
  local tabela="$1"
  sed -n "s/^INSERT INTO stg_${tabela} VALUES (\([0-9][0-9]*\),.*/\1/p" "$DIR_DADOS/finance.sql" |
    LC_ALL=C sort -n |
    awk 'NR==1{min=$1} {max=$1; n++} END{if(n>0) print min, max, n; else print "0 0 0"}'
}

sql_staging() {
  local t
  echo "SET SESSION sql_mode = 'STRICT_ALL_TABLES,NO_ENGINE_SUBSTITUTION';"
  for t in $TABELAS; do
    echo "CREATE TEMPORARY TABLE stg_${t} LIKE ${t};"
  done
  cat "$DIR_DADOS/finance.sql"
}
