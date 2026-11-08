#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'AJUDA'
Carrega mural e notificacoes no MongoDB do communication (escreve no banco).

Uso: carregar-mongo.sh [--dry-run]

Variaveis de ambiente:
  MONGO_URI   obrigatoria (inclui o banco, ex.: .../sing_communication); nao e exibida
  DIR_DADOS   arquivos gerados por exportar-postgres.sh (padrao: ./saida)

Preserva os ids originais (como _id), nao sobrescreve documentos existentes,
ajusta counters para o maior id e grava manifesto-mongo.json.
--dry-run so conta e detecta conflitos.
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
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export DIR_DADOS="${DIR_DADOS:-$AQUI/saida}"
export DRY_RUN

mongosh --nodb --quiet --file "$AQUI/carregar-mongo.js"
