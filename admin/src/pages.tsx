import { FormEvent, useEffect, useState, type ReactNode } from 'react'
import { Link, NavLink, useNavigate, useParams } from 'react-router-dom'
import { api } from './api/client'
import { loginTotalRecall } from './totalrecall'
import type { CreateOrderItemInput, Order, OrderStatus } from './types'

function money(value: number | undefined): string {
  if (value == null || Number.isNaN(value)) return '—'
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function when(value: string | undefined): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('pt-BR')
}

function statusBadge(status: string) {
  const s = status.toUpperCase()
  const cls = s === 'CALCULATED' ? 'ok' : s === 'RECEIVED' ? 'warn' : 'danger'
  return <span className={`badge ${cls}`}>{status}</span>
}

function Shell({ title, lede, children }: { title: string; lede?: string; children: ReactNode }) {
  return (
    <div className="shell">
      <header className="topbar">
        <Link to="/" className="brand">
          <span className="brand-mark" aria-hidden />
          Order Ledger
        </Link>
        <nav className="nav">
          <NavLink to="/" end>
            Pedidos
          </NavLink>
          <NavLink to="/new">Novo</NavLink>
          <NavLink to="/lookup">Consulta</NavLink>
        </nav>
      </header>
      <div className="stack">
        <h1>{title}</h1>
        {lede ? <p className="lede">{lede}</p> : null}
        {children}
      </div>
    </div>
  )
}

export function LoginPage({ onAuthenticated }: { onAuthenticated: () => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    const result = await loginTotalRecall(email.trim(), password)
    setLoading(false)

    if (!result?.valid) {
      setError('E-mail ou senha TotalRecall inválidos. Verifique suas credenciais e tente novamente.')
      return
    }

    sessionStorage.setItem('order-ledger-totalrecall-session', result.profile.email)
    onAuthenticated()
  }

  return (
    <main className="login-screen">
      <form className="panel login-panel" onSubmit={onSubmit}>
        <p className="eyebrow">Order Ledger</p>
        <h1>Acessar operações</h1>
        <p className="lede">Use seu e-mail/senha TotalRecall para acessar o console de pedidos.</p>
        <div className="field">
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </div>
        <div className="field">
          <label htmlFor="password">Senha TotalRecall</label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </div>
        {error ? <p className="error">{error}</p> : null}
        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </main>
  )
}

export function OrdersPage() {
  const [page, setPage] = useState(0)
  const [size] = useState(10)
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [rows, setRows] = useState<Order[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    api
      .listOrders({ page, size, status })
      .then((data) => {
        if (cancelled) return
        setRows(data.content)
        setTotalPages(data.totalPages)
        setTotalElements(data.totalElements)
      })
      .catch((err) => {
        if (cancelled) return
        setError(err instanceof Error ? err.message : 'Falha ao listar pedidos')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [page, size, status, tick])

  return (
    <Shell
      title="Pedidos"
      lede="Ingestão idempotente por externalOrderId, totais calculados e consulta paginada."
    >
      <div className="toolbar">
        <div className="field">
          <label htmlFor="status">Status</label>
          <select
            id="status"
            value={status}
            onChange={(e) => {
              setPage(0)
              setStatus(e.target.value as OrderStatus | '')
            }}
          >
            <option value="">Todos</option>
            <option value="RECEIVED">RECEIVED</option>
            <option value="CALCULATED">CALCULATED</option>
          </select>
        </div>
        <button type="button" className="btn ghost" onClick={() => setTick((n) => n + 1)} disabled={loading}>
          Atualizar
        </button>
        <Link className="btn" to="/new">
          Novo pedido
        </Link>
      </div>

      <div className="panel">
        {error ? <p className="error">{error}</p> : null}
        {loading ? <p className="muted">Carregando…</p> : null}
        {!loading && rows.length === 0 ? (
          <div className="empty">
            Nenhum pedido neste filtro. Crie um em Novo ou rode o seed no deploy.
          </div>
        ) : null}
        {rows.length > 0 ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>External ID</th>
                  <th>Status</th>
                  <th>Total</th>
                  <th>Criado</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((order) => (
                  <tr key={order.id}>
                    <td className="mono">{order.externalOrderId}</td>
                    <td>{statusBadge(order.status)}</td>
                    <td className="mono">{money(order.totalAmount)}</td>
                    <td>{when(order.createdAt)}</td>
                    <td>
                      <Link to={`/orders/${order.id}`}>Abrir</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="pager">
          <span className="muted">
            {totalElements} pedido(s) · página {totalPages === 0 ? 0 : page + 1} de {totalPages}
          </span>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              type="button"
              className="btn ghost"
              disabled={loading || page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Anterior
            </button>
            <button
              type="button"
              className="btn ghost"
              disabled={loading || page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Próxima
            </button>
          </div>
        </div>
      </div>
    </Shell>
  )
}

function emptyItem(): CreateOrderItemInput {
  return { productId: '', quantity: 1, unitPrice: 0 }
}

export function CreatePage() {
  const navigate = useNavigate()
  const [externalOrderId, setExternalOrderId] = useState(() => `DEMO-${Date.now()}`)
  const [items, setItems] = useState<CreateOrderItemInput[]>([
    { productId: 'SKU-100', quantity: 2, unitPrice: 19.9 },
    { productId: 'SKU-200', quantity: 1, unitPrice: 49.5 },
  ])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  function updateItem(index: number, patch: Partial<CreateOrderItemInput>) {
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, ...patch } : item)))
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setNotice(null)
    try {
      const cleaned = items
        .map((item) => ({
          productId: item.productId.trim(),
          quantity: Number(item.quantity),
          unitPrice: Number(item.unitPrice),
        }))
        .filter((item) => item.productId.length > 0)
      if (!externalOrderId.trim()) throw new Error('externalOrderId é obrigatório')
      if (cleaned.length === 0) throw new Error('Informe ao menos um item')
      const result = await api.createOrder({
        externalOrderId: externalOrderId.trim(),
        items: cleaned,
      })
      setNotice(
        result.created
          ? 'Pedido criado (HTTP 201). Idempotência ativa para reenvios com o mesmo externalOrderId.'
          : 'Replay idempotente (HTTP 200): pedido já existia com este externalOrderId.',
      )
      window.setTimeout(() => navigate(`/orders/${result.order.id}`), 700)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao criar pedido')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Shell
      title="Novo pedido"
      lede="O mesmo externalOrderId não duplica: criação devolve 201; reenvio devolve 200 com o recurso existente."
    >
      <form className="panel create-panel" onSubmit={onSubmit}>
        <div className="grid-2">
          <div className="field">
            <label htmlFor="externalOrderId">externalOrderId</label>
            <input
              id="externalOrderId"
              value={externalOrderId}
              onChange={(e) => setExternalOrderId(e.target.value)}
              required
            />
          </div>
        </div>

        <h2 style={{ fontFamily: 'var(--display)', fontSize: '1.15rem', margin: '1.25rem 0 0.6rem' }}>
          Itens
        </h2>
        <div className="items-editor">
          {items.map((item, index) => (
            <div className="item-row" key={index}>
              <div className="field">
                <label>productId</label>
                <input
                  value={item.productId}
                  onChange={(e) => updateItem(index, { productId: e.target.value })}
                  required
                />
              </div>
              <div className="field">
                <label>quantity</label>
                <input
                  type="number"
                  min={1}
                  value={item.quantity}
                  onChange={(e) => updateItem(index, { quantity: Number(e.target.value) })}
                  required
                />
              </div>
              <div className="field">
                <label>unitPrice</label>
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  value={item.unitPrice}
                  onChange={(e) => updateItem(index, { unitPrice: Number(e.target.value) })}
                  required
                />
              </div>
              <button
                type="button"
                className="btn ghost"
                onClick={() => setItems((prev) => prev.filter((_, i) => i !== index))}
                disabled={items.length <= 1}
              >
                Remover
              </button>
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', gap: '0.6rem', marginTop: '1rem', flexWrap: 'wrap' }}>
          <button type="button" className="btn ghost" onClick={() => setItems((prev) => [...prev, emptyItem()])}>
            Adicionar item
          </button>
          <button type="submit" className="btn" disabled={loading}>
            {loading ? 'Enviando…' : 'Criar pedido'}
          </button>
        </div>
        {notice ? <p className="success">{notice}</p> : null}
        {error ? <p className="error">{error}</p> : null}
      </form>
    </Shell>
  )
}

export function LookupPage() {
  const navigate = useNavigate()
  const [id, setId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const order = await api.getOrder(id.trim())
      navigate(`/orders/${order.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Pedido não encontrado')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Shell title="Consulta por UUID" lede="Busca direta em GET /orders/{id}.">
      <form className="panel" onSubmit={onSubmit}>
        <div className="field">
          <label htmlFor="orderId">ID do pedido</label>
          <input
            id="orderId"
            className="mono"
            value={id}
            onChange={(e) => setId(e.target.value)}
            placeholder="uuid"
            required
          />
        </div>
        <div style={{ marginTop: '1rem' }}>
          <button type="submit" className="btn steel" disabled={loading}>
            {loading ? 'Buscando…' : 'Consultar'}
          </button>
        </div>
        {error ? <p className="error">{error}</p> : null}
      </form>
    </Shell>
  )
}

export function OrderDetailPage() {
  const { id } = useParams()
  const [order, setOrder] = useState<Order | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    setError(null)
    api
      .getOrder(id)
      .then(setOrder)
      .catch((err) => setError(err instanceof Error ? err.message : 'Falha ao carregar'))
      .finally(() => setLoading(false))
  }, [id])

  return (
    <Shell title="Detalhe do pedido" lede={order ? `externalOrderId ${order.externalOrderId}` : undefined}>
      {loading ? <p className="muted">Carregando…</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {order ? (
        <div className="panel">
          <div className="meta-grid">
            <div className="meta-card">
              <span>Status</span>
              <strong>{statusBadge(order.status)}</strong>
            </div>
            <div className="meta-card">
              <span>Total</span>
              <strong className="mono">{money(order.totalAmount)}</strong>
            </div>
            <div className="meta-card">
              <span>Criado</span>
              <strong>{when(order.createdAt)}</strong>
            </div>
            <div className="meta-card">
              <span>Atualizado</span>
              <strong>{when(order.updatedAt)}</strong>
            </div>
          </div>
          <p className="muted mono" style={{ marginTop: 0 }}>
            id {order.id}
          </p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Qtd</th>
                  <th>Unitário</th>
                  <th>Linha</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map((item, index) => (
                  <tr key={`${item.productId}-${index}`}>
                    <td className="mono">{item.productId}</td>
                    <td className="mono">{item.quantity}</td>
                    <td className="mono">{money(item.unitPrice)}</td>
                    <td className="mono">{money(item.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ marginTop: '1rem' }}>
            <Link className="btn ghost" to="/">
              Voltar à lista
            </Link>
          </div>
        </div>
      ) : null}
    </Shell>
  )
}
