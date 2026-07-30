#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PORT="${PORT:-8080}"
HEALTH_URL="${HEALTH_URL:-http://localhost:${PORT}/actuator/health}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-120}"

if [[ ! -f .env && -f .env.example ]]; then
  cp .env.example .env
  echo "Criado .env a partir de .env.example"
fi

echo "Subindo stack (Postgres + order-service)..."
docker compose up -d --build

echo "Aguardando health em ${HEALTH_URL}..."
deadline=$((SECONDS + MAX_WAIT_SECONDS))
until curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "Timeout aguardando health (${MAX_WAIT_SECONDS}s): ${HEALTH_URL}" >&2
    docker compose ps >&2 || true
    exit 1
  fi
  sleep 2
done

echo "OK: order-service saudável em ${HEALTH_URL}"
echo "API:     http://localhost:${PORT}"
echo "Swagger: http://localhost:${PORT}/swagger-ui.html"
ADMIN_PORT="${ADMIN_PORT:-9086}"
echo "Admin:   http://localhost:${ADMIN_PORT}"
API_BASE="http://127.0.0.1:${PORT}" ./scripts/seed-demo-orders.sh || true
