#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-ghcr.io/sing-platform}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERVICOS=("$@")
if [[ ${#SERVICOS[@]} -eq 0 ]]; then
  SERVICOS=(core documents communication finance)
fi

if ! minikube status --format '{{.Host}}' 2>/dev/null | grep -q Running; then
  echo "O minikube não está rodando. Suba antes com: minikube start" >&2
  exit 1
fi

for svc in "${SERVICOS[@]}"; do
  contexto="$ROOT/services/${svc}"
  if command -v cygpath >/dev/null 2>&1; then
    contexto="$(cygpath -w "$contexto")"
  fi
  echo "==> ${REGISTRY}/${svc}:latest dentro do minikube"
  minikube image build -t "${REGISTRY}/${svc}:latest" "$contexto"
done

echo "Imagens prontas. Reinicie os pods para pegar a versão nova:"
echo "  kubectl rollout restart deployment -n sing"
