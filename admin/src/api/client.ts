import type { CreateOrderInput, CreateOrderOutcome, Order, OrderStatus, PagedOrders } from '../types'

function correlationId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `ol-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function parseError(res: Response): Promise<string> {
  try {
    const body = await res.json()
    return body.message || body.title || body.detail || JSON.stringify(body)
  } catch {
    return res.statusText || `Erro HTTP ${res.status}`
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json')
  }
  if (!headers.has('X-Correlation-Id')) {
    headers.set('X-Correlation-Id', correlationId())
  }
  const res = await fetch(`/api${path}`, { ...init, headers })
  if (!res.ok) {
    throw new Error(await parseError(res))
  }
  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

export const api = {
  listOrders: (opts: { page?: number; size?: number; status?: OrderStatus | '' } = {}) => {
    const params = new URLSearchParams()
    params.set('page', String(opts.page ?? 0))
    params.set('size', String(opts.size ?? 20))
    params.set('sort', 'createdAt,desc')
    if (opts.status) params.set('status', opts.status)
    return request<PagedOrders>(`/orders?${params.toString()}`)
  },

  getOrder: (id: string) => request<Order>(`/orders/${encodeURIComponent(id)}`),

  createOrder: async (payload: CreateOrderInput): Promise<CreateOrderOutcome> => {
    const headers = new Headers({
      'Content-Type': 'application/json',
      'X-Correlation-Id': correlationId(),
    })
    const res = await fetch('/api/orders', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    })
    if (!res.ok) {
      throw new Error(await parseError(res))
    }
    const order = (await res.json()) as Order
    return { order, created: res.status === 201 }
  },
}
