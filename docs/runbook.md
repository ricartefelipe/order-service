# Runbook

## Health / Readiness

- `GET /actuator/health`
- `GET /actuator/info`

Exemplo:

```bash
curl -sS http://localhost:8080/actuator/health
```

## Métricas

- `GET /actuator/metrics`
- `GET /actuator/prometheus`

Exemplo:

```bash
curl -sS http://localhost:8080/actuator/prometheus | head
```

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
