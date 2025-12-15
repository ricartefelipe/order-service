#!/usr/bin/env bash
set -euo pipefail

ORDER_SERVICE_URL="${ORDER_SERVICE_URL:-http://localhost:8080}"
EXTERNAL_ORDER_ID="${1:-A-$(date +%s)}"

gen_uuid() {
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen
  elif command -v python3 >/dev/null 2>&1; then
    python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
  else
    echo "corr-$(date +%s%N)"
  fi
}

CORRELATION_ID="${X_CORRELATION_ID:-$(gen_uuid)}"

payload=$(cat <<JSON
{
  "externalOrderId": "${EXTERNAL_ORDER_ID}",
  "items": [
    { "productId": "SKU-1", "quantity": 2, "unitPrice": 10.50 },
    { "productId": "SKU-2", "quantity": 1, "unitPrice": 5.00 }
  ]
}
JSON
)

echo "POST ${ORDER_SERVICE_URL}/orders"
echo "X-Correlation-Id: ${CORRELATION_ID}"
echo "externalOrderId: ${EXTERNAL_ORDER_ID}"

tmp_body="$(mktemp)"

http_code=$(curl -sS -o "${tmp_body}" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: ${CORRELATION_ID}" \
  -d "${payload}" \
  "${ORDER_SERVICE_URL}/orders")

echo "HTTP ${http_code}"
cat "${tmp_body}"
echo

rm -f "${tmp_body}"
