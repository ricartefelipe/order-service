#!/usr/bin/env bash
set -euo pipefail

ORDER_SERVICE_URL="${ORDER_SERVICE_URL:-http://localhost:8080}"
STATUS="${1:-CALCULATED}"
PAGE="${2:-0}"
SIZE="${3:-20}"
SORT="${4:-createdAt,desc}"

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

URL="${ORDER_SERVICE_URL}/orders?status=${STATUS}&page=${PAGE}&size=${SIZE}&sort=${SORT}"

echo "GET ${URL}"
echo "X-Correlation-Id: ${CORRELATION_ID}"

tmp_body="$(mktemp)"

http_code=$(curl -sS -o "${tmp_body}" -w "%{http_code}" \
  -H "X-Correlation-Id: ${CORRELATION_ID}" \
  "${URL}")

echo "HTTP ${http_code}"
cat "${tmp_body}"
echo

rm -f "${tmp_body}"
