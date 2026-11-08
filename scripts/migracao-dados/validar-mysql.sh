#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'AJUDA'
Compara origem (resumo-mysql.txt e finance.sql) com o MySQL, somente leitura.

Variaveis de ambiente: as mesmas de carregar-mysql.sh.

Confere, nas faixas de ids do manifesto-mysql.env: contagem por tabela, quantidade e soma
de amount por direction/status, soma por frequencia de sponsor, total de fiscal_document,
soma de value_transaction por tipo (arredondada a centavos) e comparacao linha a linha
de todas as colunas entre o arquivo de origem e o destino (exige MySQL 8.0.31+).
Sai com codigo 1 se algo divergir.
AJUDA
  exit 0
fi

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=comum-mysql.sh
source "$AQUI/comum-mysql.sh"

[[ -f "$MANIFESTO" ]] || { echo "manifesto-mysql.env nao encontrado; nada a validar" >&2; exit 2; }
# shellcheck disable=SC1090
source "$MANIFESTO"

esperado="$(mktemp)"
obtido="$(mktemp)"
trap 'rm -f "$esperado" "$obtido"' EXIT

{
  cat "$DIR_DADOS/resumo-mysql.txt"
  for t in $TABELAS; do
    echo "diferentes.$t|0"
  done
} | LC_ALL=C sort > "$esperado"

faixa() {
  local key
  key="$(echo "$1" | tr '[:lower:]' '[:upper:]')"
  local min="${key}_MIN" max="${key}_MAX"
  echo "id BETWEEN ${!min} AND ${!max}"
}

diferentes() {
  local t="$1"
  local f
  f="$(faixa "$t")"
  echo "SELECT CONCAT('diferentes.${t}|', COUNT(*)) FROM ((SELECT * FROM stg_${t} EXCEPT SELECT * FROM ${t} WHERE ${f}) UNION ALL (SELECT * FROM ${t} WHERE ${f} EXCEPT SELECT * FROM stg_${t})) d;"
}

{
  sql_staging
  echo "SELECT CONCAT('sponsor.total|', COUNT(*)) FROM sponsor WHERE $(faixa sponsor);"
  echo "SELECT CONCAT('sponsor.valor.', frequency, '|', SUM(amount)) FROM sponsor WHERE $(faixa sponsor) GROUP BY frequency;"
  echo "SELECT CONCAT('fiscal_document.total|', COUNT(*)) FROM fiscal_document WHERE $(faixa fiscal_document);"
  echo "SELECT CONCAT('fiscal_document.valor|', COALESCE(SUM(total_amount), 0.00)) FROM fiscal_document WHERE $(faixa fiscal_document);"
  echo "SELECT CONCAT('financial_entry.total|', COUNT(*)) FROM financial_entry WHERE $(faixa financial_entry);"
  echo "SELECT CONCAT('financial_entry.qtd.', direction, '.', status, '|', COUNT(*)) FROM financial_entry WHERE $(faixa financial_entry) GROUP BY direction, status;"
  echo "SELECT CONCAT('financial_entry.valor.', direction, '.', status, '|', SUM(amount)) FROM financial_entry WHERE $(faixa financial_entry) GROUP BY direction, status;"
  echo "SELECT CONCAT('financial_transaction.total|', COUNT(*)) FROM financial_transaction WHERE $(faixa financial_transaction);"
  echo "SELECT CONCAT('financial_transaction.valor.', type, '|', ROUND(SUM(CAST(value_transaction AS DECIMAL(14,2))), 2)) FROM financial_transaction WHERE $(faixa financial_transaction) GROUP BY type;"
  for t in $TABELAS; do
    diferentes "$t"
  done
} | mysql_cli | LC_ALL=C sort > "$obtido"

if diff "$esperado" "$obtido" > /dev/null; then
  echo "validacao mysql: OK ($(wc -l < "$esperado") verificacoes)"
else
  echo "validacao mysql: DIVERGENCIA (< esperado da origem, > encontrado no MySQL)" >&2
  diff "$esperado" "$obtido" >&2 || true
  exit 1
fi
