#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'AJUDA'
Compara origem (resumo-mongo.txt) com o MongoDB, somente leitura.

Variaveis de ambiente:
  MONGO_URI   obrigatoria; nao e exibida
  DIR_DADOS   padrao: ./saida

Confere contagens de posts, views, lidas e notificacoes, views e lidas por post,
maiores ids, counters e uma amostra de ids com campos comparados.
Sai com codigo 1 se algo divergir. Rode antes de reabrir escrita no communication:
leituras feitas depois alteram viewedAt/readAt e mudam os totais.
AJUDA
  exit 0
fi

: "${MONGO_URI:?defina MONGO_URI}"
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export DIR_DADOS="${DIR_DADOS:-$AQUI/saida}"

esperado="$(mktemp)"
obtido="$(mktemp)"
trap 'rm -f "$esperado" "$obtido"' EXIT

{
  cat "$DIR_DADOS/resumo-mongo.txt"
  echo "amostra.divergentes|0"
  echo "counters.mural_posts_cobre_max|1"
  echo "counters.notificacoes_usuario_cobre_max|1"
} | LC_ALL=C sort > "$esperado"
mongosh --nodb --quiet --file "$AQUI/validar-mongo.js" | LC_ALL=C sort > "$obtido"

if diff "$esperado" "$obtido" > /dev/null; then
  echo "validacao mongo: OK ($(wc -l < "$esperado") verificacoes)"
else
  echo "validacao mongo: DIVERGENCIA (< esperado da origem, > encontrado no Mongo)" >&2
  diff "$esperado" "$obtido" >&2 || true
  exit 1
fi
