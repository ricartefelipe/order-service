# Technology

## Stack

- **Java 21**
- **Spring Boot 3.x**
  - Spring Web (REST)
  - Spring Validation (Bean Validation)
  - Spring Data JPA + Hibernate
  - Spring Boot Actuator
- **PostgreSQL 16** (Docker)
- **Liquibase** (YAML)
- **OpenAPI** via `springdoc-openapi` (Swagger UI)
- **Testes**: JUnit 5 + Spring Boot Test + Testcontainers (PostgreSQL)

## Motivos das escolhas

### PostgreSQL 16
- Banco relacional maduro para garantir integridade (FK, UNIQUE) e consistência sob concorrência.
- Índices usados como mecanismo de idempotência (unicidade em `external_order_id`).

### JPA/Hibernate + Spring Data
- Produtividade e padrão enterprise.
- Mapeamento JPA isolado do domínio para manter a Clean Architecture.

### Liquibase
- Migrações versionadas em YAML, fáceis de revisar.
- Garante reprodutibilidade do schema local e em CI/CD.

### springdoc-openapi
- Contrato navegável e validável via Swagger UI.

### Actuator
- Endpoints de saúde e métricas essenciais para operação.

## Riscos e mitigação

### Idempotência sob alta concorrência
**Risco:** retries do Externo A podem criar duplicidade.

**Mitigação:**
- UNIQUE constraint no banco.
- `saveAndFlush` dentro de transação + fallback para leitura por `externalOrderId`.

### N+1 em listagens
**Risco:** ao listar pedidos e acessar itens, executar uma query por pedido.

**Mitigação:**
- `hibernate.default_batch_fetch_size=100` + `@BatchSize(size=100)` na coleção.
- `open-in-view=false` e mapeamento dentro de transações read-only.

### Hotspots no banco (picos)
**Risco:** picos (150k–200k pedidos/dia + bursts) podem saturar conexões.

**Mitigação:**
- Pool Hikari configurável por env var.
- Índices alinhados aos filtros principais.
- Paginação obrigatória.

### Serialização/validação
**Risco:** payloads inválidos geram inconsistência.

**Mitigação:**
- Bean Validation no request.
- Tratamento centralizado via `@RestControllerAdvice`.

## Convenções

- Dinheiro: `BigDecimal` com escala **2** e arredondamento **HALF_UP** (value object `Money`).
- Status: `RECEIVED`, `CALCULATED`.
- Correlation id: header `X-Correlation-Id` + MDC (`corr=<id>` nos logs).
