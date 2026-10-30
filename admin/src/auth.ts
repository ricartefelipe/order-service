// O Order Ledger é um ator da API como qualquer integrador: usa um token de cliente com escopos
// orders:read (lista/consulta) e orders:create (novo pedido) e só enxerga os pedidos da sua origem.
// O token fica na sessão do navegador (sessionStorage) e nunca é gravado em outro lugar.
const STORAGE_KEY = 'orderLedger.clientToken'

export function getClientToken(): string {
  try {
    return sessionStorage.getItem(STORAGE_KEY) ?? ''
  } catch {
    return ''
  }
}

export function setClientToken(token: string): void {
  try {
    if (token.trim()) sessionStorage.setItem(STORAGE_KEY, token.trim())
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    // sem storage disponível: o token vale só para esta chamada
  }
}

export function tokenSubject(token: string): string | null {
  const parts = token.split('.')
  if (parts.length !== 3) return null
  try {
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.sub === 'string' ? payload.sub : null
  } catch {
    return null
  }
}
