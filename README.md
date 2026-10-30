# Order Service

Microserviço **order** em **Java 21 + Spring Boot 4** para:

- Receber pedidos de sistemas integradores autenticados (POST /orders)
- Calcular `lineTotal` e `totalAmount` (BigDecimal, escala 2, HALF_UP) na moeda do pedido
- Persistir em PostgreSQL 16 com migrações Liquibase (YAML)
- Expor consulta por id, por referência externa e paginada, sempre no escopo da origem autenticada
- Garantir **idempotência** por origem + `externalOrderId` + fingerprint do payload, sob concorrência
- Observabilidade: correlation id via `X-Correlation-Id`, Actuator, métricas de ingestão e logs estruturados

Versão **2.0.0**: autenticação máquina a máquina obrigatória, isolamento por origem e conflito (409) para
payload diferente. Mudanças em relação à 1.x em [CHANGELOG.md](CHANGELOG.md).

## Pré-requisitos

- **Docker** e **Docker Compose**
- **Java 21**
- **Maven 3.9+**

## Como rodar (Docker Compose)

Na raiz do projeto:

```bash
cp .env.example .env   # segredo de desenvolvimento; em produção use JWKS do provedor de identidade
docker compose up -d --build
```

A aplicação sobe em:

- API: `http://127.0.0.1:8080`
- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- Order Ledger (admin UI): `http://127.0.0.1:9086`

Toda chamada a `/orders` exige um token de cliente. Para emitir um no modo de segredo compartilhado:

```bash
set -a; . ./.env; set +a
export ORDER_TOKEN="$(./scripts/issue-token.sh system-a)"   # sub=system-a, escopos orders:create e orders:read
```

O Order Ledger também é um cliente: cole um token em **Credencial** e ele passa a ver os pedidos da origem desse token.

Para AWS portfolio, use `docker compose -f docker-compose.yml -f docker-compose.aws.yml up -d --build`
(API `:8082`, admin `:9086`).

## Como rodar testes

```bash
mvn verify
```

A suíte sobe PostgreSQL 16 com Testcontainers (Docker obrigatório). Ela cobre cálculo, limites, fingerprint,
autenticação (401/403), isolamento por origem, replay (200), conflito (409), concorrência e a equivalência
entre o fingerprint calculado em Java e o calculado em SQL pela migration 002. O número de testes executados
é lido dos relatórios do Surefire; zero testes ou testes pulados falham a CI.

## Como gerar o build

```bash
mvn package
```

## Scripts de simulação (integrações A e B)

> Os scripts abaixo simulam o **Sistema Externo A** enviando pedidos e o **Sistema Externo B** consultando pedidos calculados. Todos exigem `ORDER_TOKEN` (veja acima). Como cada token tem uma origem, um pedido enviado com o token de `system-a` só aparece para consultas com token de `system-a`.

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

Todas as rotas abaixo exigem `Authorization: Bearer <token>`; o contrato completo, incluindo o token, está em
[docs/integration.md](docs/integration.md).

### POST /orders

Request (escopo `orders:create`):

```json
{
  "externalOrderId": "A-123456",
  "currency": "BRL",
  "items": [
    { "productId": "SKU-1", "quantity": 2, "unitPrice": 10.50 },
    { "productId": "SKU-2", "quantity": 1, "unitPrice": 5.00 }
  ]
}
```

`currency` é opcional (padrão `ORDERS_DEFAULT_CURRENCY`). A origem do pedido é o `sub` do token.

Respostas:

- **201 Created**: quando cria um novo pedido
- **200 OK**: mesmo `externalOrderId` da mesma origem com o **mesmo payload** (replay idempotente; devolve o original)
- **409 Conflict**: mesmo `externalOrderId` da mesma origem com **payload diferente**; o pedido original não muda
- **400 Bad Request**: payload inválido (quantidade zero ou acima de 1.000.000, preço negativo ou com mais de 2 casas, moeda desconhecida, mais de 500 itens, total acima de 99999999999999999.99)
- **401 / 403**: sem token válido / sem o escopo

### GET /orders/{id}

- **200 OK**: retorna o pedido (escopo `orders:read`)
- **404 Not Found**: id inexistente **ou pertencente a outra origem**

### GET /orders

Query params (escopo `orders:read`; só pedidos da origem do token):

- `status` (opcional): `RECEIVED` | `CALCULATED`
- `externalOrderId` (opcional): busca por referência externa
- `page` (default 0)
- `size` (default 20, máximo 200)
- `sort` (default `createdAt,desc`)

Resposta paginada (exemplo):

```json
{
  "content": [
    {
      "id": "uuid",
      "origin": "system-a",
      "externalOrderId": "A-123456",
      "currency": "BRL",
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

`health` e `info` são públicos; `metrics` e `prometheus` exigem um token válido. A métrica
`orders.ingestion{outcome=created|replayed|conflicted}` conta os desfechos da ingestão.

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

## Autenticação máquina a máquina

- Token JWT com `iss`, `aud`, `exp`, `sub` (origem) e `scope` (`orders:create`, `orders:read`).
- Chave: `ORDERS_AUTH_JWKS_URI` (provedor OIDC, fluxo client credentials) **ou** `ORDERS_AUTH_SECRET`
  (HMAC, mínimo 32 bytes). Exatamente um dos dois; o processo não sobe sem isso.
- Rotação: no modo JWKS, girar a chave no provedor; no modo segredo, trocar o segredo e reiniciar.
- O segredo de `.env.example` é de desenvolvimento. Decisão e limites em [ADR-003](docs/adr/ADR-003-client-authentication-and-origin.md).

## Idempotência (origem + externalOrderId + fingerprint)

1. **Banco**: UNIQUE em `orders(origin, external_order_id)`
2. **Aplicação**: tenta inserir em transação (`saveAndFlush`); na violação de unicidade lê o pedido existente
   em transação separada e compara o `fingerprint` (SHA-256 versionado da moeda e dos itens em forma canônica):
   igual devolve o original (200); diferente responde 409 sem alterar nada.

Funciona sob concorrência (o índice único do PostgreSQL serializa o mesmo par origem + id). Detalhes em
[ADR-002](docs/adr/ADR-002-idempotent-ingestion.md).

## Considerações de performance (150k–200k pedidos/dia)

- Índices:
  - `unique(origin, external_order_id)`
  - `index(origin, status, created_at)` e `index(origin, created_at)`
  - `index(order_items.order_id)`
- Pool Hikari configurável por variáveis de ambiente
- Listagem paginada otimizada para evitar N+1: pagina por IDs (ordenação determinística com `id` como desempate) e carrega `orders + items` em batch (IN + fetch)
- `hibernate.default_batch_fetch_size=100` + `@BatchSize` como salvaguarda adicional
- `open-in-view=false` e mapeamento dentro de transações read-only


## CI

O workflow `ci` (`.github/workflows/ci.yml`) roda em pull requests e nos pushes em `develop` e `main`:

- `backend`: `mvn verify` com Testcontainers PostgreSQL 16, contagem dos testes executados e scan das
  dependências resolvidas com Trivy (HIGH/CRITICAL bloqueiam).
- `admin`: `npm ci`, type check e build do Order Ledger.
- `images`: build das duas imagens finais, scan com Trivy (HIGH/CRITICAL bloqueiam) e smoke
  (`scripts/smoke.sh`) contra PostgreSQL real: 401 sem token, 201, 200 no replay, 409 no conflito, 403 sem
  escopo e 404 ao ler pedido de outra origem.

O workflow "Sync status" é manual e somente leitura: compara `develop` com `main` e não abre PR nem faz merge.
