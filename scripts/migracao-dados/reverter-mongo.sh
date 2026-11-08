#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'AJUDA'
Remove do MongoDB apenas o que a carga migrou (escreve no banco).

Uso: reverter-mongo.sh [--dry-run]

Variaveis de ambiente:
  MONGO_URI   obrigatoria; nao e exibida
  DIR_DADOS   onde esta manifesto-mongo.json (padrao: ./saida)
  CONFIRMAR   deve ser "sim" para apagar de fato

Apaga documentos com _migracao:true dentro das faixas do manifesto e volta o contador
ao valor anterior somente se ele ainda estiver no valor aplicado pela carga.
Nunca remove colecao nem banco.
AJUDA
  exit 0
fi

DRY_RUN=0
case "${1:-}" in
  "") ;;
  --dry-run) DRY_RUN=1 ;;
  *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
esac

: "${MONGO_URI:?defina MONGO_URI}"
if [[ "$DRY_RUN" == "0" && "${CONFIRMAR:-}" != "sim" ]]; then
  echo "defina CONFIRMAR=sim para executar o rollback, ou use --dry-run" >&2
  exit 2
fi
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export DIR_DADOS="${DIR_DADOS:-$AQUI/saida}"
export DRY_RUN

mongosh --nodb --quiet --file "$AQUI/reverter-mongo.js"
