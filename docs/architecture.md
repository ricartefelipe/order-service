# Architecture

Este documento descreve a arquitetura do microserviço **order** usando o modelo **C4** (Context, Container, Component).

## Context (C4)

```mermaid
C4Context
  title Order Service - System Context

  Person(extA, "Produto Externo A", "Sistema que disponibiliza pedidos")
  Person(extB, "Produto Externo B", "Sistema que consome pedidos calculados")

  System(orderSvc, "order-service", "Recebe pedidos, calcula totais, persiste e expõe consultas")
  SystemDb(postgres, "PostgreSQL 16", "Armazenamento de pedidos e itens")

  Rel(extA, orderSvc, "POST /orders", "HTTP/JSON")
  Rel(extB, orderSvc, "GET /orders, GET /orders/{id}", "HTTP/JSON")
  Rel(orderSvc, postgres, "Read/Write", "JDBC")
```

## Containers (C4)

```mermaid
C4Container
  title Order Service - Containers

  Person(extA, "Produto Externo A")
  Person(extB, "Produto Externo B")

  Container(orderSvc, "order-service", "Java 21 + Spring Boot 3.x", "API REST, idempotência, cálculo e persistência")
  ContainerDb(postgres, "PostgreSQL 16", "Banco relacional", "Tabelas orders e order_items")

  Rel(extA, orderSvc, "Envia pedidos", "HTTP/JSON")
  Rel(extB, orderSvc, "Consulta pedidos", "HTTP/JSON")
  Rel(orderSvc, postgres, "JPA/Hibernate + JDBC", "SQL")
```

## Components (C4)

```mermaid
C4Component
  title Order Service - Components (Ports & Adapters)

  Container_Boundary(orderSvc, "order-service") {

    Component(web, "Web Adapter", "Spring MVC", "Controllers, request/response models, mappers")
    Component(appCreate, "CreateOrderService", "Application", "Caso de uso: ingestão idempotente e cálculo")
    Component(appGet, "GetOrderService", "Application", "Caso de uso: consulta por id")
    Component(appList, "ListOrdersService", "Application", "Caso de uso: listagem paginada")

    Component(domain, "Domain Model", "Domain", "Order, OrderItem, Money, invariantes, OrderStatus")

    Component(persistence, "Persistence Adapter", "Spring Data JPA", "Mapeamento domínio <-> JPA, repositórios")
    ComponentDb(postgres, "PostgreSQL", "Database", "Persistência")

    Rel(web, appCreate, "CreateOrderUseCase")
    Rel(web, appGet, "GetOrderUseCase")
    Rel(web, appList, "ListOrdersUseCase")

    Rel(appCreate, persistence, "OrderRepositoryPort")
    Rel(appGet, persistence, "OrderRepositoryPort")
    Rel(appList, persistence, "OrderRepositoryPort")

    Rel(appCreate, domain, "Cria e valida Order")
    Rel(appGet, domain, "Retorna Order")
    Rel(appList, domain, "Retorna Order")

    Rel(persistence, postgres, "SQL", "JDBC")
  }
```

## Notas relevantes

- **Clean Architecture**: domínio não depende de Spring/JPA.
- **Idempotência**: `externalOrderId` com UNIQUE no banco + tentativa de insert em transação e fallback para leitura.
- **Observabilidade**: `X-Correlation-Id` + MDC (logs) + Actuator.
