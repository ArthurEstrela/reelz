import type { AuthSession } from '../types/auth'

const AUTH_SESSION_KEY = 'reelz.auth.session'
const AUTH_SESSION_EXPIRED_KEY = 'reelz.auth.expired'

export const AUTH_SESSION_EXPIRED_EVENT = 'reelz:auth-session-expired'

function isAuthSession(value: unknown): value is AuthSession {
  if (typeof value !== 'object' || value === null) return false

  const session = value as Partial<AuthSession>
  return (
    typeof session.accessToken === 'string' &&
    session.accessToken.length > 0 &&
    typeof session.expiresAt === 'number' &&
    typeof session.user?.id === 'string' &&
    typeof session.user.email === 'string' &&
    typeof session.user.onboardingCompleted === 'boolean' &&
    (session.user.role === 'USER' || session.user.role === 'ADMIN')
  )
}

export function getAuthSession(): AuthSession | null {
  const serializedSession = sessionStorage.getItem(AUTH_SESSION_KEY)
  if (!serializedSession) return null

  try {
    const session: unknown = JSON.parse(serializedSession)
    if (!isAuthSession(session) || session.expiresAt <= Date.now()) {
      clearAuthSession()
      return null
    }
    return session
  } catch {
    clearAuthSession()
    return null
  }
}

export function saveAuthSession(session: AuthSession): void {
  sessionStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session))
}

export function clearAuthSession(): void {
  sessionStorage.removeItem(AUTH_SESSION_KEY)
}

export function markAuthSessionExpired(): void {
  sessionStorage.setItem(AUTH_SESSION_EXPIRED_KEY, 'true')
}

export function consumeAuthSessionExpired(): boolean {
  const expired = sessionStorage.getItem(AUTH_SESSION_EXPIRED_KEY) === 'true'
  sessionStorage.removeItem(AUTH_SESSION_EXPIRED_KEY)
  return expired
}
