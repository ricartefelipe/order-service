#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

API_BASE="${API_BASE:-http://127.0.0.1:8082}"
COUNT="$(curl -fsS "${API_BASE}/orders?page=0&size=1" | sed -n 's/.*"totalElements":\([0-9]*\).*/\1/p' | head -1)"
COUNT="${COUNT:-0}"

if [[ "${COUNT}" != "0" ]]; then
  echo "Seed ignorado: já existem ${COUNT} pedido(s) em ${API_BASE}"
  exit 0
fi

echo "Sem pedidos — criando demos em ${API_BASE}..."

post() {
  local external="$1"
  shift
  curl -fsS -X POST "${API_BASE}/orders" \
    -H "Content-Type: application/json" \
    -H "X-Correlation-Id: seed-${external}" \
    -d "$@" >/dev/null
  echo "  ok ${external}"
}

post "DEMO-ALPHA-001" '{"externalOrderId":"DEMO-ALPHA-001","items":[{"productId":"SKU-ALPHA","quantity":2,"unitPrice":29.90},{"productId":"SKU-BETA","quantity":1,"unitPrice":15.00}]}'
post "DEMO-BETA-002" '{"externalOrderId":"DEMO-BETA-002","items":[{"productId":"SKU-GAMMA","quantity":3,"unitPrice":9.99}]}'
post "DEMO-GAMMA-003" '{"externalOrderId":"DEMO-GAMMA-003","items":[{"productId":"SKU-DELTA","quantity":1,"unitPrice":199.00},{"productId":"SKU-EPS","quantity":4,"unitPrice":12.50}]}'

echo "Seed concluído."
