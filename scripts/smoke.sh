#!/usr/bin/env bash
# Smoke da imagem final contra PostgreSQL real: token, criação, replay, conflito e isolamento por origem.
# Uso: ORDERS_AUTH_SECRET=... ./scripts/smoke.sh <imagem>
set -euo pipefail

IMAGE="${1:?informe a imagem do order-service}"
: "${ORDERS_AUTH_SECRET:?defina ORDERS_AUTH_SECRET}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NET="orders-smoke-$$"
PORT="${SMOKE_PORT:-18080}"
API="http://127.0.0.1:${PORT}"

cleanup() {
  docker rm -f "smoke-api-$$" "smoke-db-$$" >/dev/null 2>&1 || true
  docker network rm "$NET" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker network create "$NET" >/dev/null
docker run -d --name "smoke-db-$$" --network "$NET" \
  -e POSTGRES_DB=orders -e POSTGRES_USER=orders -e POSTGRES_PASSWORD=orders \
  --health-cmd "pg_isready -U orders -d orders" --health-interval 2s --health-retries 30 \
  postgres:16 >/dev/null
for _ in $(seq 1 40); do
  [ "$(docker inspect -f '{{.State.Health.Status}}' "smoke-db-$$")" = healthy ] && break
  sleep 2
done
docker run -d --name "smoke-api-$$" --network "$NET" -p "127.0.0.1:${PORT}:8080" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://smoke-db-$$:5432/orders" \
  -e ORDERS_AUTH_SECRET="$ORDERS_AUTH_SECRET" \
  "$IMAGE" >/dev/null
for _ in $(seq 1 60); do
  curl -fsS "$API/actuator/health" >/dev/null 2>&1 && break
  sleep 2
done
curl -fsS "$API/actuator/health"; echo

TOKEN_A="$("$ROOT/scripts/issue-token.sh" system-a)"
TOKEN_B="$("$ROOT/scripts/issue-token.sh" system-b "orders:read")"
PAYLOAD='{"externalOrderId":"SMOKE-1","items":[{"productId":"SKU-1","quantity":2,"unitPrice":10.50}]}'
CHANGED='{"externalOrderId":"SMOKE-1","items":[{"productId":"SKU-1","quantity":3,"unitPrice":10.50}]}'

expect() { # expect <esperado> <obtido> <descrição>
  if [ "$1" != "$2" ]; then printf 'FALHA %s: esperado %s, obtido %s\n' "$3" "$1" "$2" >&2; exit 1; fi
  printf 'ok %s -> %s\n' "$3" "$2"
}
code() { curl -sS -o /dev/null -w '%{http_code}' "$@"; }

expect 401 "$(code -H 'Content-Type: application/json' -d "$PAYLOAD" "$API/orders")" "sem token"
expect 201 "$(code -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' -d "$PAYLOAD" "$API/orders")" "criação"
expect 200 "$(code -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' -d "$PAYLOAD" "$API/orders")" "replay"
expect 409 "$(code -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' -d "$CHANGED" "$API/orders")" "conflito"
expect 403 "$(code -H "Authorization: Bearer $TOKEN_B" -H 'Content-Type: application/json' -d "$PAYLOAD" "$API/orders")" "escopo insuficiente"
ID="$(curl -fsS -H "Authorization: Bearer $TOKEN_A" "$API/orders?externalOrderId=SMOKE-1" | python3 -c 'import json,sys; print(json.load(sys.stdin)["content"][0]["id"])')"
expect 200 "$(code -H "Authorization: Bearer $TOKEN_A" "$API/orders/$ID")" "leitura pela origem dona"
expect 404 "$(code -H "Authorization: Bearer $TOKEN_B" "$API/orders/$ID")" "leitura por outra origem"
echo "smoke ok"
