# Runbook

## Health / Readiness

- `GET /actuator/health`
- `GET /actuator/info`

Exemplo:

```bash
curl -sS http://localhost:8080/actuator/health
```

## Métricas

- `GET /actuator/metrics` e `GET /actuator/prometheus` exigem um token de cliente válido.
- `orders_ingestion_total{outcome="created|replayed|conflicted"}` conta os desfechos da ingestão. Uma subida de
  `conflicted` indica integrador reenviando ids com conteúdo diferente; `replayed` são retries legítimos.

```bash
curl -sS -H "Authorization: Bearer $ORDER_TOKEN" http://localhost:8080/actuator/prometheus | grep orders_ingestion
```

## Credenciais

- Modo JWKS: `ORDERS_AUTH_JWKS_URI`, `ORDERS_AUTH_ISSUER`, `ORDERS_AUTH_AUDIENCE`; integradores recebem client
  credentials no provedor com `sub` = origem e escopos `orders:create` / `orders:read`.
- Modo segredo: `ORDERS_AUTH_SECRET` (>= 32 bytes); `scripts/issue-token.sh <origem> ["escopos"] [ttl]`.
- Rotação: publicar nova chave no provedor; ou trocar o segredo, reiniciar e reemitir tokens.
- O processo recusa subir sem exatamente uma fonte de chave ou com segredo curto.

## Logs

Formato inclui correlation id:

```
... [corr=<correlationId>] - request method=POST path=/orders status=201 durationMs=12
```

### Correlation Id

- Envie `X-Correlation-Id` em chamadas do cliente para correlacionar logs e respostas.

## Troubleshooting

### Aplicação não sobe / falha no Liquibase

1. Verifique se o Postgres está pronto:
   ```bash
   docker compose ps
   docker compose logs postgres --tail=200
   ```
2. Verifique variáveis de ambiente do datasource no container:
   ```bash
   docker compose logs order-service --tail=200
   ```

### 500 em POST /orders

1. Verifique o log com o correlation id devolvido na resposta.
2. Verifique se o schema foi aplicado (Liquibase).

### Lentidão em listagens

- Reduza `size` (paginação)
- Garanta que está ordenando por campo indexado (`createdAt`)
- Ajuste pool Hikari (em produção, dimensionar conforme throughput/latência e limites do Postgres)

## Operação local (Docker Compose)

- Subir:
  ```bash
  docker compose up -d
  ```
- Acompanhar logs:
  ```bash
  docker compose logs -f order-service
  ```
- Derrubar:
  ```bash
  docker compose down
  ```

### 401 / 403

- `401`: token ausente, vencido, de outro emissor/audiência ou assinatura inválida. Confira `iss`, `aud`, `exp`
  e a fonte de chave configurada.
- `403`: token válido sem o escopo da operação (`orders:create` para POST, `orders:read` para GET).

### 409 em POST /orders

O integrador reenviou um `externalOrderId` com conteúdo diferente. O pedido original está intacto; consulte-o com
`GET /orders?externalOrderId=<id>` usando o token da mesma origem e compare com o que o integrador enviou.
Não edite o banco: se o novo conteúdo for o correto, o integrador deve usar um novo `externalOrderId`.

### 404 em GET /orders/{id} para um id que existe

O pedido pertence a outra origem. Use o token da origem dona (`origin` na resposta de criação).

## Reconciliação sem editar o banco

- Pedido "sumiu": `GET /orders?externalOrderId=<id>` com o token da origem. Sem resultado em nenhuma origem
  conhecida, o POST nunca foi aceito (procure o `X-Correlation-Id` nos logs).
- Divergência de total: compare `items` da resposta com o enviado; o total é sempre `sum(quantity * unitPrice)`.
- Pedidos da 1.x: ficam na origem `ORDERS_LEGACY_ORIGIN` (padrão `legacy`); um token com esse `sub` os lê.

## Backup e restauração

```bash
docker compose exec postgres pg_dump -U orders -d orders -Fc > orders-$(date +%F).dump
# restauração em banco vazio
docker compose exec -T postgres pg_restore -U orders -d orders --no-owner < orders-YYYY-MM-DD.dump
```

A restauração preserva a unicidade `(origin, external_order_id)` e os fingerprints: um reenvio após o restore
responde 200 (mesmo conteúdo) ou 409 (diferente), nunca cria duplicata. Valide pela API (`GET /orders/{id}` e um
POST de replay), não por leitura direta de tabela.

## Upgrade 1.x -> 2.0.0

1. Faça backup.
2. Defina `ORDERS_LEGACY_ORIGIN` e `ORDERS_LEGACY_CURRENCY` (padrões `legacy` e `BRL`) e a fonte de chave.
3. Suba a 2.0.0: a migration 002 adiciona origem, moeda e fingerprint aos pedidos existentes e troca a unicidade.
4. Emita um token com `sub` igual à origem legada para consultar os pedidos antigos.
