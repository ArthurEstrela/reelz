const PRODUCT_SESSION_KEY = 'cinegiro.product.session'
const LEGACY_PRODUCT_SESSION_KEY = 'reelz.product.session'

export function getProductSessionId(): string {
  const current = sessionStorage.getItem(PRODUCT_SESSION_KEY)
    ?? sessionStorage.getItem(LEGACY_PRODUCT_SESSION_KEY)
  if (current) {
    sessionStorage.setItem(PRODUCT_SESSION_KEY, current)
    sessionStorage.removeItem(LEGACY_PRODUCT_SESSION_KEY)
    return current
  }

  const sessionId = crypto.randomUUID()
  sessionStorage.setItem(PRODUCT_SESSION_KEY, sessionId)
  return sessionId
}
