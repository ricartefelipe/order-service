# ADR-001: Clean Architecture (Ports & Adapters)

## Status

Accepted

## Context

O serviço precisa:

- Separar regras de negócio (domínio) de detalhes de framework (Spring, JPA)
- Facilitar testes unitários do cálculo e testes de integração do fluxo
- Manter o código evolutivo, minimizando acoplamento a camadas externas

## Decision

Adotar **Clean Architecture** com **Ports & Adapters**, estruturando o código em quatro áreas principais:

1. `domain`
   - Entidades do domínio (`Order`, `OrderItem`)
   - Value objects (`Money`)
   - Enum de status (`OrderStatus`)
   - Invariantes do domínio

2. `application`
   - Casos de uso (`CreateOrderUseCase`, `GetOrderUseCase`, `ListOrdersUseCase`)
   - DTOs de comando e resultado
   - Ports (interfaces) para persistência e consulta (`OrderRepositoryPort`)

3. `adapters`
   - `web`: controllers + request/response models + mappers
   - `persistence`: entidades JPA + repositories Spring Data + mappers + adapter que implementa ports

4. `config`
   - Cross-cutting concerns: correlation id, OpenAPI, error handling, auditing

## Consequences

### Positive
- Domínio isolado de Spring/JPA
- Maior testabilidade e legibilidade
- Facilidade para trocar detalhes de persistência/integração no futuro

### Negative
- Mais classes/mappers (overhead inicial)
- Necessidade de disciplina para não vazar JPA/Spring no domínio
