export type OrderStatus = 'RECEIVED' | 'CALCULATED' | string

export type OrderItem = {
  productId: string
  quantity: number
  unitPrice: number
  lineTotal?: number
}

export type Order = {
  id: string
  externalOrderId: string
  status: OrderStatus
  totalAmount: number
  items: OrderItem[]
  createdAt: string
  updatedAt: string
}

export type PagedOrders = {
  content: Order[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type CreateOrderItemInput = {
  productId: string
  quantity: number
  unitPrice: number
}

export type CreateOrderInput = {
  externalOrderId: string
  items: CreateOrderItemInput[]
}

export type CreateOrderOutcome = {
  order: Order
  created: boolean
}
