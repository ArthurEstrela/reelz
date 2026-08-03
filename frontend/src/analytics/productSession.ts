const PRODUCT_SESSION_KEY = 'reelz.product.session'

export function getProductSessionId(): string {
  const current = sessionStorage.getItem(PRODUCT_SESSION_KEY)
  if (current) return current

  const sessionId = crypto.randomUUID()
  sessionStorage.setItem(PRODUCT_SESSION_KEY, sessionId)
  return sessionId
}
