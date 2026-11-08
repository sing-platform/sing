#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'AJUDA'
Carrega o financeiro legado no MySQL do servico .NET (escreve no banco).

Uso: carregar-mysql.sh [--dry-run]

Variaveis de ambiente:
  MYSQL_HOST (127.0.0.1)  MYSQL_PORT (3306)  MYSQL_USER  MYSQL_DATABASE
  MYSQL_PWD               senha (variavel padrao do cliente mysql; nunca exibida)
  DIR_DADOS               arquivos gerados por exportar-postgres.sh (padrao: ./saida)

Pre-requisito: as migracoes EF do servico ja foram aplicadas (tabelas existem, schema vazio
ou sem ids conflitantes). Aborta se algum id do intervalo migrado ja existir no destino.
Carrega sponsor, fiscal_document, financial_entry e financial_transaction numa transacao
unica, preservando ids e FKs, e depois ajusta AUTO_INCREMENT para max(id)+1.
Grava manifesto-mysql.env com as faixas carregadas. --dry-run so confere e conta.
AJUDA
  exit 0
fi

DRY_RUN=0
case "${1:-}" in
  "") ;;
  --dry-run) DRY_RUN=1 ;;
  *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
esac

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=comum-mysql.sh
source "$AQUI/comum-mysql.sh"

[[ -f "$DIR_DADOS/finance.sql" ]] || { echo "finance.sql nao encontrado em $DIR_DADOS" >&2; exit 2; }
[[ ! -f "$MANIFESTO" ]] || { echo "ja existe $MANIFESTO; carga anterior nao revertida" >&2; exit 4; }

conflitos=0
declare -A MIN MAX TOTAL
for t in $TABELAS; do
  read -r MIN[$t] MAX[$t] TOTAL[$t] < <(faixa_origem "$t")
  existentes=0
  if [[ "${TOTAL[$t]}" != "0" ]]; then
    existentes="$(mysql_cli -e "SELECT COUNT(*) FROM ${t} WHERE id BETWEEN ${MIN[$t]} AND ${MAX[$t]}")"
  fi
  echo "$t: origem=${TOTAL[$t]} faixa=${MIN[$t]}-${MAX[$t]} ids_ocupados_no_destino=$existentes"
  if [[ "$existentes" != "0" ]]; then
    conflitos=$((conflitos + 1))
  fi
done

if [[ "$conflitos" != "0" ]]; then
  echo "abortado: o destino ja tem ids na faixa migrada" >&2
  exit 4
fi

if [[ "$DRY_RUN" == "1" ]]; then
  echo "dry-run: nada foi gravado"
  exit 0
fi

{
  echo "GERADO_EM=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  for t in $TABELAS; do
    key="$(echo "$t" | tr '[:lower:]' '[:upper:]')"
    echo "${key}_MIN=${MIN[$t]}"
    echo "${key}_MAX=${MAX[$t]}"
    echo "${key}_TOTAL=${TOTAL[$t]}"
  done
} > "$MANIFESTO"

if ! {
  sql_staging
  echo "START TRANSACTION;"
  for t in $TABELAS; do
    echo "INSERT INTO ${t} SELECT * FROM stg_${t};"
  done
  echo "COMMIT;"
} | mysql_cli; then
  rm -f "$MANIFESTO"
  echo "falha na carga; transacao desfeita e manifesto removido" >&2
  exit 5
fi

for t in $TABELAS; do
  proximo="$(mysql_cli -e "SELECT COALESCE(MAX(id), 0) + 1 FROM ${t}")"
  mysql_cli -e "ALTER TABLE ${t} AUTO_INCREMENT = ${proximo}"
  echo "$t: carregado ${TOTAL[$t]} linhas; AUTO_INCREMENT=${proximo}"
done
echo "manifesto em $MANIFESTO"
