#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'AJUDA'
Remove do MySQL apenas as linhas que a carga migrou (escreve no banco).

Uso: reverter-mysql.sh [--dry-run]

Variaveis de ambiente: as de carregar-mysql.sh, mais
  CONFIRMAR   deve ser "sim" para apagar de fato

Apaga por faixa de ids registrada em manifesto-mysql.env, numa transacao, na ordem
financial_entry, fiscal_document, sponsor, financial_transaction. Se houver linhas novas
referenciando as migradas, a FK impede e a transacao inteira e desfeita.
Nunca faz DROP nem TRUNCATE; nao reduz AUTO_INCREMENT.
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

[[ -f "$MANIFESTO" ]] || { echo "manifesto-mysql.env nao encontrado; nada a reverter" >&2; exit 2; }
if [[ "$DRY_RUN" == "0" && "${CONFIRMAR:-}" != "sim" ]]; then
  echo "defina CONFIRMAR=sim para executar o rollback, ou use --dry-run" >&2
  exit 2
fi
# shellcheck disable=SC1090
source "$MANIFESTO"

ordem="financial_entry fiscal_document sponsor financial_transaction"

filtro() {
  local key
  key="$(echo "$1" | tr '[:lower:]' '[:upper:]')"
  local min="${key}_MIN" max="${key}_MAX" total="${key}_TOTAL"
  if [[ "${!total}" == "0" ]]; then
    echo "1 = 0"
  else
    echo "id BETWEEN ${!min} AND ${!max}"
  fi
}

if [[ "$DRY_RUN" == "1" ]]; then
  for t in $ordem; do
    n="$(mysql_cli -e "SELECT COUNT(*) FROM ${t} WHERE $(filtro "$t")")"
    echo "$t: removeria $n"
  done
  exit 0
fi

{
  echo "START TRANSACTION;"
  for t in $ordem; do
    echo "DELETE FROM ${t} WHERE $(filtro "$t");"
  done
  echo "COMMIT;"
} | mysql_cli

mv "$MANIFESTO" "$MANIFESTO.revertido-$(date -u +%Y%m%dT%H%M%SZ)"
echo "rollback concluido"
