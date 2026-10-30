# Order Service — Template de Ingestão Idempotente de Pedidos (Spring Boot)

> Microserviço de referência **pronto para produção** para ingestão e cálculo de pedidos entre sistemas: **idempotência sob concorrência, Clean Architecture, ADRs e runbook de operação**. Você parte de uma base sólida, não de uma folha em branco.
>
> Documentação técnica completa: [`README.md`](README.md).

---

## Que problema resolve

Integrar dois sistemas via pedidos parece trivial — até o primeiro pedido entrar **duplicado** porque o Sistema A reenviou a requisição. As partes que dão errado quase nunca são o "happy path", e sim:

- **Pedido duplicado** quando o emissor reenvia o mesmo `externalOrderId` (timeout, retry, concorrência).
- **Cálculo de totais inconsistente** (arredondamento, escala monetária).
- **Falta de rastreabilidade** quando algo falha em produção (sem correlation id, sem runbook).
- **Semanas gastas montando a fundação** em vez de entregar a integração.

O Order Service entrega essa fundação confiável, do jeito que se coloca em produção — com a documentação que prova como e por quê.

---

## O que vem no kit

| # | Recurso | Descrição |
|---|---------|-----------|
| 1 | Ingestão idempotente | Deduplicação por origem + `externalOrderId` + fingerprint do payload **sob concorrência**: o mesmo pedido nunca é criado duas vezes (201 na criação, 200 no reenvio igual, 409 quando o mesmo id chega com dados diferentes). |
| 2 | Cálculo correto de totais | `lineTotal` e `totalAmount` em `BigDecimal`, escala 2, `HALF_UP`, moeda por pedido e limites de quantidade e valor validados no servidor. |
| 3 | Consulta para integração | `GET /orders/{id}` e `GET /orders` paginado, filtrável por status e por referência externa. Cada integrador só enxerga os próprios pedidos. |
| 4 | Segurança máquina a máquina | Token JWT por integrador (OIDC client credentials via JWKS ou segredo compartilhado), com emissor, audiência, expiração e escopos `orders:create` / `orders:read`. |
| 5 | Observabilidade | Correlation id via `X-Correlation-Id`, Actuator, métrica de desfechos da ingestão e logs estruturados. |
| 6 | Migrações versionadas | PostgreSQL 16 + Liquibase (YAML) — schema versionado, com upgrade dos dados da 1.x. |
| 7 | Documentação madura | ADRs (Clean Architecture, ingestão idempotente, autenticação por origem), `architecture.md`, `integration.md`, `runbook.md`, `technology.md`. |
| 8 | Demonstrável e verificado | Scripts simulando os Sistemas Externos A e B, Swagger UI, Order Ledger, Docker Compose e CI real (build, testes com PostgreSQL, scan das imagens e smoke). |
| 9 | Order Ledger | Console de operação (lista/criação/consulta) que autentica como um cliente da API com o próprio token e vê só a sua origem. |

**Stack:** Java 21 · Spring Boot 4 · Spring Security (resource server) · PostgreSQL 16 · Liquibase · OpenAPI/Swagger · Docker Compose.

---

## Para quem é

- Devs sêniores e tech leads que precisam de uma **referência de microserviço idempotente** em Spring Boot.
- Times com integração de pedidos entre sistemas (ERP ↔ e-commerce ↔ logística).
- Quem quer um exemplo real de **Clean Architecture + ADRs + runbook** bem feitos para acelerar o time.

**Não é para:** quem busca um produto SaaS gerenciado (aqui você tem o código, com controle total), nem para quem não pretende manter código Java/Spring.

---

## Começando

```bash
cp .env.example .env
docker compose up -d --build
```

- API: `http://127.0.0.1:8080`
- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- Order Ledger (admin): `http://127.0.0.1:9086`

Simule a integração ponta a ponta:

```bash
set -a; . ./.env; set +a
export ORDER_TOKEN="$(./scripts/issue-token.sh system-a)"
./scripts/a_send_order.sh A-123456   # Sistema A envia pedido
./scripts/b_pull_orders.sh           # consulta os pedidos da mesma origem
```

Passo a passo, contratos e regras: [`README.md`](README.md) e [`docs/`](docs/).

---

## Licença

Produto comercial licenciado — **não** é open source. Resumo:

- ✅ Usar, modificar e incorporar em produtos **próprios e de clientes**, sem limite de produtos finais.
- ✅ Implantar os produtos finais em produção.
- ❌ Revender/redistribuir o código como template, boilerplate ou produto concorrente.
- ❌ Publicar o código-fonte como base reutilizável aberta.

Termos completos em [`LICENSE`](LICENSE).

---

## Combine e economize — Kit de Padrões de Produção

O Order Service faz par com o **AssinaFlow** (cobrança recorrente com outbox, idempotência e retry). Juntos formam um **Kit de Padrões de Produção** em Spring Boot — ingestão idempotente + ciclo de cobrança confiável — ideal para quem está montando a fundação de um produto. Bundle com desconto sob consulta.

Os dois kits são independentes: não existe integração técnica pronta entre eles (nenhum contrato, evento ou
chamada entre os serviços). Ligar pedidos a assinaturas é um trabalho de integração sobre as APIs documentadas
de cada um; veja a seção correspondente em [`docs/integration.md`](docs/integration.md).

---

## Implantação assistida (opcional)

A licença é entregue "as-is" com documentação. Se quiser apoio para colocar em produção ou adaptar ao seu domínio, há um pacote separado de **Implantação Assistida**: walkthrough técnico (1h), adaptação e suporte a dúvidas por período definido.

Contato: **felipericartem@gmail.com**

---

## Autor

**Felipe Ricarte Magalhães** — Senior Backend Engineer · Software Architect (hands-on). Mais de 17 anos em tecnologia, com foco em backend e arquitetura aplicada: APIs, integrações, mensageria, idempotência e observabilidade em ambientes corporativos.
