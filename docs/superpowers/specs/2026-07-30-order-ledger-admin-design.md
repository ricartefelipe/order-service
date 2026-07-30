# Order Ledger Admin UI — design

## Goal

Console ops for Order Service: list, create and query orders against the live API, with clear idempotency feedback via `externalOrderId`. Deployed on portfolio EC2 `:9086`.

## Visual direction — Order Ledger

Distinct from AssinaFlow (sage/teal light) and Cards Desk (dark cyan/gold).

- Mood: industrial warehouse ledger / ops desk
- Palette: cool slate paper (`#e4e8ef` → `#cfd6e2`), charcoal ink (`#12161f`), copper accent (`#c45a2c`), steel secondary (`#3a5070`)
- Type: Outfit (display) + IBM Plex Sans (UI)
- Motion: list stagger fade, status chip pulse, create panel slide-in
- Avoid: purple/indigo gradients, cream+terracotta broadsheet, dark-mode default, teal clones

## Stack

Vite + React 19 + TypeScript + react-router, nginx static + `/api` proxy to `order-service` (same pattern as Cards Desk).

## Auth

API has no auth today. No login screen. Do not invent credentials or show demo user/password on any surface.

## Screens

1. **Pedidos** — paginated list (`page`, `size`, `status` filter RECEIVED|CALCULATED), open detail
2. **Novo pedido** — `externalOrderId` + line items; show 201 created vs 200 replay
3. **Consulta** — fetch by UUID; optional deep-link `/orders/:id`

## Deploy

- Container `order-admin` on host port **9086**
- SG `portfolio-apps-sg` TCP 9086 open
- Hub Pages + EC2 `/opt/portfolio/index.html` link Order Ledger → `:9086` (Swagger remains `:8082`)
- Seed demo orders when list is empty

## Out of scope

Login/Keycloak, edit/cancel orders, analytics dashboards, multi-tenant branding.
