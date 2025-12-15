# ADR-002: Idempotência na ingestão de pedidos (externalOrderId)

## Status

Accepted

## Context

O sistema recebe pedidos do Externo A com possíveis **retries** e picos de concorrência.

Requisitos:

- `externalOrderId` é obrigatório
- Se o mesmo `externalOrderId` for reenviado, **não pode duplicar**
- Sob concorrência (duas requisições simultâneas), deve existir **apenas 1 registro** no banco
- API deve retornar:
  - **201** para criação nova
  - **200** para idempotência (retorno do recurso existente)

## Decision

Implementar idempotência combinando garantias do banco e fallback na aplicação:

1. **Banco**
   - UNIQUE constraint em `orders.external_order_id`

2. **Aplicação (transactional)**
   - Tenta inserir (`saveAndFlush`)
   - Em caso de violação de unicidade (`DataIntegrityViolationException`), faz leitura por `externalOrderId` e retorna o pedido existente

A leitura após violação ocorre em uma transação read-only separada (via `TransactionTemplate`) para evitar efeitos colaterais do flush/exception no contexto de persistência.

## Consequences

### Positive
- Correto sob concorrência: PostgreSQL garante exclusão mútua no índice único
- Sem locks explícitos
- Sem necessidade de cache/distributed lock

### Negative
- Em retries, uma requisição pode pagar o custo de:
  - 1 tentativa de insert + 1 leitura
- Dependência do mecanismo de integridade do banco (o que é aceitável neste contexto)
