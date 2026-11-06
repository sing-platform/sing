#!/usr/bin/env bash
# Publica core, documents e communication em ghcr.io/sing-platform/<servico>.
# Plataforma linux/amd64, que e a dos nos t3.medium do EKS.
#
#   export GHCR_USER=seu-usuario
#   export GHCR_TOKEN=...   # precisa de write:packages
#   ./scripts/build-push.sh
#
# Nao rode este script no CI com action de terceiro. O workflow
# .github/workflows/imagens.yml chama este mesmo arquivo no runner.

set -euo pipefail

REGISTRY="${REGISTRY:-ghcr.io/sing-platform}"
TAG="${TAG:-latest}"
PLATFORM="${PLATFORM:-linux/amd64}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -z "${GHCR_USER:-}" || -z "${GHCR_TOKEN:-}" ]]; then
  echo "Defina GHCR_USER e GHCR_TOKEN antes de rodar." >&2
  exit 1
fi

echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin

for svc in core documents communication; do
  echo "==> ${REGISTRY}/${svc}:${TAG} (${PLATFORM})"
  tags=(-t "${REGISTRY}/${svc}:${TAG}")
  if [[ -n "${GITHUB_SHA:-}" ]]; then
    tags+=(-t "${REGISTRY}/${svc}:${GITHUB_SHA}")
  fi
  docker buildx build --platform "$PLATFORM" "${tags[@]}" --push "$ROOT/services/${svc}"
done
