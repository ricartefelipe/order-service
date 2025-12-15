# Integration Guide

Este documento descreve os contratos e comportamentos de integração para **Externo A** (envio) e **Externo B** (consulta).

## Headers

### X-Correlation-Id

- Entrada: opcional
- Se ausente, o serviço gera um UUID
- Saída: sempre devolve `X-Correlation-Id`

Exemplo:

```bash
curl -i \
  -H 'X-Correlation-Id: 8c1ef5e9-3e7d-4d5b-b2b0-0b2b8f2b6b2a' \
  http://localhost:8080/actuator/health
```

## Externo A -> Order Service

### POST /orders

Request (contrato assumido):

```json
{
  "externalOrderId": "A-123456",
  "items": [
    { "productId": "SKU-1", "quantity": 2, "unitPrice": 10.50 },
    { "productId": "SKU-2", "quantity": 1, "unitPrice": 5.00 }
  ]
}
```

Regras:

- `externalOrderId` obrigatório
- `items` não pode ser vazio
- `quantity` > 0
- `unitPrice` >= 0 e com até 2 casas decimais

Cálculo:

- `lineTotal = unitPrice * quantity` (BigDecimal, escala 2, HALF_UP)
- `totalAmount = sum(lineTotal)`
- `status` final: `CALCULATED`

Respostas:

- **201 Created**: criação de um novo pedido
- **200 OK**: idempotência (pedido já existia com o mesmo `externalOrderId`)
- **400 Bad Request**: payload inválido

## Idempotência e deduplicação

Estratégia implementada (obrigatória):

1. **UNIQUE constraint** em `orders.external_order_id`
2. Na aplicação:
   - em transação, tenta inserir (`saveAndFlush`)
   - se violar unicidade, busca por `externalOrderId` e retorna o existente

Garantias:

- Funciona sob concorrência (o índice de unicidade do PostgreSQL garante exclusão mútua para o mesmo `externalOrderId`).
- Em retries do Externo A, o serviço responde de forma consistente e sem duplicar.

## Externo B <- Order Service

### GET /orders/{id}

- **200 OK**: retorna o pedido calculado
- **404 Not Found**: não existe

Exemplo:

```bash
curl -sS http://localhost:8080/orders/<uuid>
```

### GET /orders (paginado)

Query params:

- `status` (opcional): `RECEIVED` | `CALCULATED`
- `page` (default 0)
- `size` (default 20, max 200)
- `sort` (default `createdAt,desc`)

Sort suportado:

- `createdAt`
- `externalOrderId`
- `status`
- `totalAmount`

Resposta:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 123,
  "totalPages": 7
}
```

Exemplo:

```bash
curl -sS 'http://localhost:8080/orders?status=CALCULATED&page=0&size=20&sort=createdAt,desc'
```

## Erros

Formato padrão:

```json
{
  "timestamp": "2025-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/orders",
  "correlationId": "...",
  "fieldErrors": [
    { "field": "items[0].quantity", "message": "quantity must be > 0" }
  ]
}
```
