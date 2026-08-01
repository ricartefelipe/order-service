export type TotalRecallLoginResult =
  | {
      valid: true
      profile: { id: string; name: string; email: string }
      system: { slug: string; name: string }
      expiresAt: string
    }
  | { valid: false; reason?: string }

const DEFAULT_BASE_URL = 'https://54.94.163.136.sslip.io'

function totalRecallBaseUrl(): string {
  return (import.meta.env.VITE_TOTALRECALL_URL as string | undefined)?.trim().replace(/\/$/, '') || DEFAULT_BASE_URL
}

export async function loginTotalRecall(email: string, password: string): Promise<TotalRecallLoginResult | null> {
  try {
    const response = await fetch(`${totalRecallBaseUrl()}/api/v1/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, system: 'order-service' }),
    })
    return (await response.json()) as TotalRecallLoginResult
  } catch {
    return null
  }
}
