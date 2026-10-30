import { getClientToken } from '../auth'
import type { CreateOrderInput, CreateOrderOutcome, Order, OrderStatus, PagedOrders } from '../types'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

function correlationId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `ol-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function parseError(res: Response): Promise<string> {
  if (res.status === 401) return 'Token de cliente ausente ou inválido. Informe um token em Credencial.'
  if (res.status === 403) return 'O token não tem o escopo necessário para esta operação.'
  try {
    const body = await res.json()
    return body.message || body.title || body.detail || JSON.stringify(body)
  } catch {
    return res.statusText || `Erro HTTP ${res.status}`
  }
}

function headers(init: RequestInit): Headers {
  const result = new Headers(init.headers)
  if (!result.has('Content-Type') && init.body) {
    result.set('Content-Type', 'application/json')
  }
  if (!result.has('X-Correlation-Id')) {
    result.set('X-Correlation-Id', correlationId())
  }
  const token = getClientToken()
  if (token) {
    result.set('Authorization', `Bearer ${token}`)
  }
  return result
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`/api${path}`, { ...init, headers: headers(init) })
  if (!res.ok) {
    throw new ApiError(res.status, await parseError(res))
  }
  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

export const api = {
  listOrders: (opts: { page?: number; size?: number; status?: OrderStatus | ''; externalOrderId?: string } = {}) => {
    const params = new URLSearchParams()
    params.set('page', String(opts.page ?? 0))
    params.set('size', String(opts.size ?? 20))
    params.set('sort', 'createdAt,desc')
    if (opts.status) params.set('status', opts.status)
    if (opts.externalOrderId) params.set('externalOrderId', opts.externalOrderId)
    return request<PagedOrders>(`/orders?${params.toString()}`)
  },

  getOrder: (id: string) => request<Order>(`/orders/${encodeURIComponent(id)}`),

  createOrder: async (payload: CreateOrderInput): Promise<CreateOrderOutcome> => {
    const init: RequestInit = { method: 'POST', body: JSON.stringify(payload) }
    const res = await fetch('/api/orders', { ...init, headers: headers(init) })
    if (!res.ok) {
      throw new ApiError(res.status, await parseError(res))
    }
    const order = (await res.json()) as Order
    return { order, created: res.status === 201 }
  },
}
