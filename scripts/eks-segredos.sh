#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SAIDA="$ROOT/deploy/eks/secrets.local.yaml"

if [[ -e "$SAIDA" && "${FORCAR:-}" != "1" ]]; then
  echo "$SAIDA já existe. Trocar as senhas com os bancos no ar quebra o login deles." >&2
  echo "Para gerar de novo mesmo assim: FORCAR=1 $0" >&2
  exit 1
fi

senha() { openssl rand -hex "$1"; }

DB=$(senha 16)
DOCS_DB=$(senha 16)
RABBIT=$(senha 16)
MINIO=$(senha 16)
JWT=$(senha 28)
MONGO_DOCS=$(senha 16)
MONGO_COMM=$(senha 16)
KEYCLOAK=$(senha 16)
FIN_DB=$(senha 16)
FIN_ROOT=$(senha 16)

umask 077
cat > "$SAIDA" <<YAML
apiVersion: v1
kind: Secret
metadata:
  name: sing-secrets
stringData:
  DB_PASSWORD: "$DB"
  DOCS_DB_PASSWORD: "$DOCS_DB"
  RABBITMQ_PASSWORD: "$RABBIT"
  S3_SECRET_KEY: "$MINIO"
  MINIO_ROOT_PASSWORD: "$MINIO"
  JWT_SECRET: "$JWT"
  MONGO_DOCUMENTS_PASSWORD: "$MONGO_DOCS"
  MONGO_COMMUNICATION_PASSWORD: "$MONGO_COMM"
  KEYCLOAK_PASSWORD: "$KEYCLOAK"
  FINANCE_DB_PASSWORD: "$FIN_DB"
  FINANCE_DB_ROOT_PASSWORD: "$FIN_ROOT"
  DATABASE_URL: "postgresql+psycopg://sing:$DOCS_DB@postgres-documents:5432/sing_documents"
  MONGO_COMPROVANTES_URI: "mongodb://documents:$MONGO_DOCS@mongo-documents:27017/sing_documents_comprovantes?authSource=admin"
  MONGO_URI: "mongodb://communication:$MONGO_COMM@mongo-communication:27017/sing_communication?authSource=admin"
  ConnectionStrings__Finance: "Server=mysql-finance;Port=3306;Database=sing_finance;User=sing;Password=$FIN_DB;"
YAML

echo "Segredos gerados em deploy/eks/secrets.local.yaml (fora do git). Os valores não foram impressos."
