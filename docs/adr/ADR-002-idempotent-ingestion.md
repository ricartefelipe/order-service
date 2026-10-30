# ADR-002: Idempotência na ingestão de pedidos (externalOrderId)

## Status

Accepted

## Context

O sistema recebe pedidos do Externo A com possíveis **retries** e picos de concorrência.

Requisitos:

- `externalOrderId` é obrigatório e único **por origem** (ADR-003)
- Se o mesmo `externalOrderId` for reenviado com o mesmo conteúdo, **não pode duplicar**
- Se o mesmo `externalOrderId` chegar com conteúdo diferente, o serviço **não pode** devolver silenciosamente
  o pedido antigo nem sobrescrevê-lo
- Sob concorrência (duas requisições simultâneas), deve existir **apenas 1 registro** no banco
- API deve retornar:
  - **201** para criação nova
  - **200** para replay (retorno do recurso existente, mesmo payload)
  - **409** para conflito (mesmo id, payload diferente)

## Decision

Implementar idempotência combinando garantias do banco e fallback na aplicação:

1. **Banco**
   - UNIQUE constraint em `orders(origin, external_order_id)`
   - `orders.fingerprint` (SHA-256 hex) e `orders.fingerprint_version` (`v1`)

2. **Fingerprint canônico (domínio, `PayloadFingerprint`)**
   - Texto `v1\n<currency>\n` + uma linha `productId<TAB>quantity<TAB>unitPrice` por item, linhas ordenadas
     bytewise; SHA-256 do UTF-8. Ordem dos itens e escala textual do preço não importam; conteúdo importa.
   - A migration 002 calcula o mesmo texto em SQL para os pedidos da 1.x; um teste de integração confere que
     o valor gravado pela aplicação é igual ao da expressão SQL.

3. **Aplicação (transactional)**
   - Tenta inserir (`saveAndFlush`)
   - Em caso de violação de unicidade (`DataIntegrityViolationException` com SQLSTATE 23505), lê o pedido
     existente por origem + `externalOrderId` e compara o fingerprint: igual devolve o existente; diferente
     lança `IdempotencyConflictException` (409) sem tocar no pedido

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
- Mudar a forma canônica exige nova versão de fingerprint e recálculo por migration; Java e SQL mudam juntos
