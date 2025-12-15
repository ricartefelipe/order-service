# Order Service

Microserviço **order** em **Java 21 + Spring Boot 3.x** para:

- Receber pedidos de um Sistema Externo A (POST /orders)
- Calcular `lineTotal` e `totalAmount` (BigDecimal, escala 2, HALF_UP)
- Persistir em PostgreSQL 16 com migrações Liquibase (YAML)
- Expor APIs de consulta para um Sistema Externo B (GET /orders/{id} e GET /orders paginado)
- Garantir **idempotência/deduplicação** via `externalOrderId` sob concorrência
- Observabilidade: correlation id via `X-Correlation-Id`, Actuator e logs estruturados

## Pré-requisitos

- **Docker** e **Docker Compose**
- **Java 21**
- **Maven 3.9+**

## Como rodar (Docker Compose)

Na raiz do projeto:

```bash
docker compose up -d
```

A aplicação sobe em:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Como rodar testes

```bash
mvn test
```

## Como gerar o build

```bash
mvn package
```

## Scripts de simulação (integrações A e B)

> Os scripts abaixo simulam o **Sistema Externo A** enviando pedidos e o **Sistema Externo B** consultando pedidos calculados.

### Enviar um pedido (Externo A)

```bash
./scripts/a_send_order.sh
```

Ou informando um `externalOrderId` específico:

```bash
./scripts/a_send_order.sh A-123456
```

### Consultar pedidos paginados (Externo B)

```bash
./scripts/b_pull_orders.sh
```

Parâmetros:

```bash
./scripts/b_pull_orders.sh CALCULATED 0 20 createdAt,desc
```

> Versões PowerShell: `./scripts/a_send_order.ps1` e `./scripts/b_pull_orders.ps1`.

## Endpoints

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

Respostas:

- **201 Created**: quando cria um novo pedido
- **200 OK**: quando o `externalOrderId` já existe (idempotência)

### GET /orders/{id}

- **200 OK**: retorna o pedido
- **404 Not Found**: id inexistente

### GET /orders

Query params:

- `status` (opcional): `RECEIVED` | `CALCULATED`
- `page` (default 0)
- `size` (default 20, máximo 200)
- `sort` (default `createdAt,desc`)

Resposta paginada (exemplo):

```json
{
  "content": [
    {
      "id": "uuid",
      "externalOrderId": "A-123456",
      "status": "CALCULATED",
      "totalAmount": 26.00,
      "items": [
        { "productId": "SKU-1", "quantity": 2, "unitPrice": 10.50, "lineTotal": 21.00 },
        { "productId": "SKU-2", "quantity": 1, "unitPrice": 5.00, "lineTotal": 5.00 }
      ],
      "createdAt": "2025-01-01T10:00:00Z",
      "updatedAt": "2025-01-01T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

## Observabilidade

### Correlation Id

- Header de entrada: `X-Correlation-Id`
- Se ausente, o serviço gera um UUID
- O mesmo header é devolvido na resposta
- O valor é propagado nos logs via **MDC** (`corr=<id>`)

### Actuator

- Health: `GET /actuator/health`
- Info: `GET /actuator/info`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

## Arquitetura (Clean Architecture / Ports & Adapters)

Estrutura (obrigatória):

- `domain`: entidades, value objects (Money), invariantes, enum de status
- `application`: casos de uso, portas de persistência/consulta, DTOs
- `adapters`:
  - `web`: controllers, request/response models, mappers
  - `persistence`: entidades JPA (isoladas), repositórios, mappers
- `config`: correlation id filter, OpenAPI, error handling, observabilidade, JPA auditing

Detalhes completos:

- `docs/architecture.md`
- `docs/integration.md`
- `docs/technology.md`
- `docs/runbook.md`
- `docs/adr/*`

## Idempotência / Deduplicação (externalOrderId)

Regras implementadas:

1. **Banco**: constraint UNIQUE em `orders.external_order_id`
2. **Aplicação**:
   - tenta inserir em transação (`saveAndFlush`)
   - se violar unicidade, busca por `externalOrderId` e retorna o recurso existente

Isso funciona sob concorrência (PostgreSQL faz locking no índice de unicidade).

## Considerações de performance (150k–200k pedidos/dia)

- Índices:
  - `unique(external_order_id)`
  - `index(status, created_at)`
  - `index(created_at)`
  - `index(order_items.order_id)`
- Pool Hikari configurável por variáveis de ambiente
- `hibernate.default_batch_fetch_size=100` + `@BatchSize` para evitar N+1 em listagens
- `open-in-view=false` e mapeamento dentro de transações read-only

