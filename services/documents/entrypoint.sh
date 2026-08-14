#!/bin/sh
set -e

# Cada servico migra o proprio banco no start — ninguem toca no schema do outro.
echo "[documents] aplicando migrations..."
alembic upgrade head

echo "[documents] iniciando API..."
exec uvicorn app.main:app --host 0.0.0.0 --port 8000
