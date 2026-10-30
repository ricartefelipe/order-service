# Changelog

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). O repositório não usa tags; a
versão está em `pom.xml` e no OpenAPI (`info.version`).

## [2.0.0] - 2026-10-30

### Added

- Autenticação máquina a máquina: token JWT de cliente (JWKS de provedor OIDC ou segredo HMAC), com emissor,
  audiência, expiração, origem (`sub`) e escopos `orders:create` / `orders:read`. `401` e `403` em JSON.
- Isolamento por origem: todo pedido pertence ao `sub` do token; consulta por id, por referência e paginada só
  devolvem pedidos da própria origem.
- `currency` por pedido (ISO 4217, padrão `ORDERS_DEFAULT_CURRENCY`) e `origin` na resposta.
- Fingerprint versionado do payload: mesmo `externalOrderId` com payload diferente responde `409 Conflict`
  sem alterar o pedido original.
- `GET /orders?externalOrderId=` para busca por referência; ordenação determinística com `id` como desempate.
- Limites validados no servidor: 500 itens, quantidade até 1.000.000, total até 99999999999999999.99.
- Métrica `orders.ingestion{outcome}` (created, replayed, conflicted).
- Migration 002: colunas `origin`, `currency`, `fingerprint_version` e `fingerprint`, unicidade
  `(origin, external_order_id)`, índices por origem e upgrade dos pedidos da 1.x com fingerprint calculado em SQL.
- CI real: build, testes com PostgreSQL via Testcontainers, contagem de testes, scan de dependências e das
  imagens finais com Trivy (HIGH/CRITICAL bloqueiam), build do Order Ledger e smoke da imagem.
- `scripts/issue-token.sh` e `scripts/smoke.sh`; Order Ledger com página Credencial.

### Changed

- Spring Boot 3.3.0 -> 4.0.9; Testcontainers 2; springdoc 3. Imagens finais em Alpine com usuário sem privilégios.
- `docker compose` exige `ORDERS_AUTH_SECRET` (ou JWKS) e publica portas só em 127.0.0.1.
- Scripts de simulação exigem `ORDER_TOKEN`.
- Workflow "Sync main with develop" convertido em relatório manual somente leitura; não abre PR nem faz merge.

### Removed

- API aberta: nenhuma rota `/orders` responde sem token.
- Proxy padrão do Vite apontando para um host público; agora `http://localhost:8080`.

## [1.0.0]

- Ingestão idempotente por `externalOrderId`, cálculo de totais, consulta paginada, Liquibase, Actuator, Order Ledger.
