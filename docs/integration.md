# Integration Guide

Contratos e comportamentos de integração do Order Service 2.x para sistemas emissores (envio) e consumidores
(consulta). A versão da API é `2.0.0`; a política de versionamento está no fim do documento.

## Autenticação (máquina a máquina)

Toda rota `/orders` exige `Authorization: Bearer <token>`. O token é um JWT com:

| Claim | Uso |
|---|---|
| `iss` | deve ser igual a `ORDERS_AUTH_ISSUER` |
| `aud` | deve conter `ORDERS_AUTH_AUDIENCE` |
| `exp` | expiração; tokens vencidos recebem 401 |
| `sub` | **origem** do integrador. Todo pedido pertence a esta origem e só é visível para ela |
| `scope` | `orders:create` para criar, `orders:read` para consultar (separados por espaço) |

Fontes de chave aceitas pelo serviço (exatamente uma):

- **Provedor de identidade (recomendado em produção):** `ORDERS_AUTH_JWKS_URI` aponta para o JWKS do
  provedor OIDC; cada integrador recebe um client credentials com o `sub`/escopos acima.
- **Segredo compartilhado:** `ORDERS_AUTH_SECRET` (HS256, mínimo 32 bytes) para instalações sem provedor.
  `scripts/issue-token.sh <origem> ["escopos"] [ttl]` emite tokens com esse segredo.

Respostas: `401` sem token, token inválido, vencido, de outro emissor ou outra audiência; `403` com token
válido sem o escopo da operação. Erros de autenticação usam o mesmo formato JSON das demais respostas.

Rotação: no modo JWKS, publicar a nova chave no provedor (o serviço busca o JWKS); no modo segredo, trocar
`ORDERS_AUTH_SECRET` e reiniciar, reemitindo os tokens.

## Headers

### X-Correlation-Id

- Entrada: opcional
- Se ausente, o serviço gera um UUID
- Saída: sempre devolve `X-Correlation-Id`

```bash
curl -i -H 'X-Correlation-Id: 8c1ef5e9-3e7d-4d5b-b2b0-0b2b8f2b6b2a' http://127.0.0.1:8080/actuator/health
```

## Emissor -> Order Service

### POST /orders (escopo `orders:create`)

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

Regras:

- `externalOrderId` obrigatório, até 100 caracteres, único **por origem**
- `currency` opcional, código ISO 4217 de 3 letras; padrão `ORDERS_DEFAULT_CURRENCY`
- `items` de 1 a 500 entradas
- `productId` obrigatório, até 100 caracteres
- `quantity` de 1 a 1.000.000
- `unitPrice` >= 0, até 17 dígitos inteiros e 2 decimais
- `totalAmount` até 99999999999999999.99; acima disso o pedido é recusado (400)

Responsabilidade pelo preço: o preço unitário é informado pelo integrador autenticado; o serviço valida
formato e limites e calcula os totais, mas não consulta catálogo. O `sub` do token identifica quem informou.

Cálculo:

- `lineTotal = unitPrice * quantity` (BigDecimal, escala 2, HALF_UP)
- `totalAmount = sum(lineTotal)`
- `status` final: `CALCULATED`

Respostas:

- **201 Created**: criação de um novo pedido (`Location: /orders/{id}`)
- **200 OK**: replay idempotente (mesmo `externalOrderId` da mesma origem com o mesmo payload); corpo igual ao original
- **409 Conflict**: mesmo `externalOrderId` da mesma origem com payload diferente; nada é alterado
- **400 Bad Request**: payload inválido
- **401 / 403**: autenticação / escopo

## Idempotência e deduplicação

1. **UNIQUE** em `orders(origin, external_order_id)`
2. **Fingerprint** do payload: SHA-256 (versão `v1`) de `v1\n<currency>\n` seguido de uma linha
   `productId<TAB>quantity<TAB>unitPrice` por item, linhas ordenadas bytewise. A ordem dos itens no JSON e a
   escala textual do preço (`5` ou `5.00`) não mudam o fingerprint; quantidade, preço, produto, moeda ou um
   item a mais mudam.
3. Na aplicação: insere em transação; na violação de unicidade lê o pedido existente em transação separada e
   compara o fingerprint: igual devolve 200 com o original, diferente devolve 409.

Garantias:

- Sob concorrência (o índice único serializa o mesmo par origem + id); uma única linha é criada.
- Em retries após timeout, o emissor recebe 200 com o pedido original, inclusive `id` e `createdAt`.
- Duas origens podem usar o mesmo `externalOrderId` sem colisão e sem enxergar o pedido uma da outra.

## Consumidor <- Order Service

### GET /orders/{id} (escopo `orders:read`)

- **200 OK**: pedido da origem do token
- **404 Not Found**: não existe ou pertence a outra origem

### GET /orders (paginado, escopo `orders:read`)

Query params:

- `status` (opcional): `RECEIVED` | `CALCULATED`
- `externalOrderId` (opcional): busca por referência externa dentro da origem
- `page` (default 0), `size` (default 20, max 200), `sort` (default `createdAt,desc`)

Sort suportado: `createdAt`, `externalOrderId`, `status`, `totalAmount`. O `id` é sempre o último critério,
então a ordem é estável entre páginas.

```bash
curl -sS -H "Authorization: Bearer $ORDER_TOKEN" \
  'http://127.0.0.1:8080/orders?status=CALCULATED&page=0&size=20&sort=createdAt,desc'
curl -sS -H "Authorization: Bearer $ORDER_TOKEN" 'http://127.0.0.1:8080/orders?externalOrderId=A-123456'
```

Resposta:

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
      "items": [ { "productId": "SKU-1", "quantity": 2, "unitPrice": 10.50, "lineTotal": 21.00 } ],
      "createdAt": "2026-10-30T10:00:00Z",
      "updatedAt": "2026-10-30T10:00:00Z"
    }
  ],
  "page": 0, "size": 20, "totalElements": 123, "totalPages": 7
}
```

## Estado

`RECEIVED -> CALCULATED` acontece antes da primeira gravação; o estado persistido é sempre `CALCULATED` e não
há transições posteriores, cancelamento, eventos ou outbox nesta versão. Histórico de estados não se aplica.

## Erros

```json
{
  "timestamp": "2026-10-30T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Order with externalOrderId A-123456 already exists with a different payload",
  "path": "/orders",
  "correlationId": "...",
  "fieldErrors": []
}
```

Nenhuma resposta ou log inclui stack trace, token ou credencial de banco.

## Versionamento e depreciação

- A versão da API está no OpenAPI (`/v3/api-docs`, campo `info.version`) e segue o `CHANGELOG.md`.
- Campos novos são aditivos e não mudam a versão maior. Consumidores devem ignorar campos desconhecidos.
- Mudança incompatível (remoção de campo, mudança de semântica, novo requisito obrigatório) sobe a versão
  maior, é anunciada no `CHANGELOG.md` e mantém a versão anterior disponível por um ciclo de release antes
  da remoção, salvo correção de segurança.
- A 2.0.0 é incompatível com a 1.x: token obrigatório, unicidade por origem e 409 para payload diferente.

## AssinaFlow

Não há integração técnica entre o Order Service e o AssinaFlow: nenhum dos dois chama o outro, consome eventos
ou compartilha banco, e não existe contrato de pedido para assinatura validado entre eles. A oferta comercial
em conjunto é um bundle de dois kits independentes. Quem precisar ligar pedidos a assinaturas usa as APIs
documentadas de cada produto a partir da versão/SHA que licenciar; esse trabalho de integração não está
incluído e não é afirmado aqui como compatível.
