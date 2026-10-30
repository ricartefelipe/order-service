# ADR-003: Autenticação máquina a máquina e isolamento por origem

## Status

Accepted

## Context

A API era aberta: qualquer processo com acesso de rede criava e lia pedidos de todos. O produto é um kit de
integração entre sistemas, sem usuários finais; a identidade relevante é a do sistema integrador. A oferta
comercial exige implantação exposta e integradores distintos sem vazamento entre eles.

## Decision

- **Token JWT de cliente** validado pelo Spring Security como resource server: emissor, audiência, expiração e
  assinatura. O claim `sub` é a **origem** do integrador; o claim `scope` autoriza `orders:create` e
  `orders:read`. A chave vem de um JWKS de provedor OIDC (client credentials) ou, em instalações sem provedor,
  de um segredo HMAC de pelo menos 32 bytes. Exatamente uma fonte é configurada; o processo não sobe sem ela.
- **Origem como atributo do pedido**: `orders.origin` guarda o `sub` de quem criou. A unicidade passa a ser
  `(origin, external_order_id)` e toda leitura (por id, por referência, paginada) filtra pela origem do token.
  Pedido de outra origem responde 404, não 403, para não revelar existência.
- **Rotas públicas**: `/`, Swagger UI, OpenAPI, `health` e `info`. `metrics` e `prometheus` exigem token.
- **Order Ledger é um cliente**: usa um token com os mesmos escopos e vê só a sua origem. Não há login de
  consumidor nem papéis de usuário.
- **Dados da 1.x** recebem a origem `ORDERS_LEGACY_ORIGIN` (padrão `legacy`) na migration 002; só um token
  com esse `sub` os enxerga.

## Consequences

### Positive
- Implantação exposta sem API aberta; isolamento entre integradores garantido pelo banco e pela consulta.
- Sem sessão, sem estado no serviço, sem dependência obrigatória de um provedor de identidade específico.
- Rotação de chaves pelo provedor (JWKS) sem reiniciar.

### Negative
- Incompatível com clientes 1.x: todos precisam de token.
- No modo segredo compartilhado, qualquer portador do segredo emite tokens para qualquer origem; esse modo é
  para desenvolvimento e instalações pequenas, não para múltiplos parceiros em produção.
- Não há revogação individual de token antes do `exp`; use validade curta.
