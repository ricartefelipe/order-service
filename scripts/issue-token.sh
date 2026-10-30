#!/usr/bin/env bash
# Emite um token de cliente HS256 para instalações sem provedor de identidade (segredo compartilhado).
# Uso: ORDERS_AUTH_SECRET=... ./scripts/issue-token.sh <origem> [escopos] [ttl-segundos]
#   escopos: lista separada por espaço; padrão "orders:create orders:read"
# Em produção prefira um provedor OIDC com client credentials e ORDERS_AUTH_JWKS_URI.
set -euo pipefail

ORIGIN="${1:?informe a origem (sub) do token}"
SCOPES="${2:-orders:create orders:read}"
TTL="${3:-3600}"
ISSUER="${ORDERS_AUTH_ISSUER:-order-service-local}"
AUDIENCE="${ORDERS_AUTH_AUDIENCE:-order-service}"
: "${ORDERS_AUTH_SECRET:?defina ORDERS_AUTH_SECRET (>= 32 bytes)}"

ORIGIN="$ORIGIN" SCOPES="$SCOPES" TTL="$TTL" ISSUER="$ISSUER" AUDIENCE="$AUDIENCE" python3 - <<'PY'
import base64, hashlib, hmac, json, os, time

def b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()

now = int(time.time())
header = b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
claims = {
    "iss": os.environ["ISSUER"],
    "aud": [os.environ["AUDIENCE"]],
    "sub": os.environ["ORIGIN"],
    "scope": os.environ["SCOPES"],
    "iat": now,
    "exp": now + int(os.environ["TTL"]),
}
payload = b64(json.dumps(claims, separators=(",", ":")).encode())
signature = hmac.new(os.environ["ORDERS_AUTH_SECRET"].encode(), f"{header}.{payload}".encode(), hashlib.sha256).digest()
print(f"{header}.{payload}.{b64(signature)}")
PY
