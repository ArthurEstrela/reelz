import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { loginRequest, registerRequest } from '../services/authService'
import {
  AUTH_SESSION_EXPIRED_EVENT,
  clearAuthSession,
  getAuthSession,
  saveAuthSession,
} from '../storage/authStorage'
import type { RegisterRequest } from '../types/api'
import type { AuthSession } from '../types/auth'
import { AuthContext, type AuthContextValue } from './authContextDefinition'

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(() => getAuthSession())

  const logout = useCallback(() => {
    clearAuthSession()
    setSession(null)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await loginRequest({ email, password })
    const nextSession: AuthSession = {
      accessToken: response.accessToken,
      expiresAt: Date.now() + response.expiresInSeconds * 1_000,
      user: {
        id: response.userId,
        email: email.trim().toLowerCase(),
        onboardingCompleted: response.onboardingCompleted,
      },
    }
    saveAuthSession(nextSession)
    setSession(nextSession)
  }, [])

  const register = useCallback(async (payload: RegisterRequest) => {
    await registerRequest(payload)
  }, [])

  const markOnboardingCompleted = useCallback(() => {
    setSession((currentSession) => {
      if (!currentSession) return null
      const nextSession: AuthSession = {
        ...currentSession,
        user: {
          ...currentSession.user,
          onboardingCompleted: true,
        },
      }
      saveAuthSession(nextSession)
      return nextSession
    })
  }, [])

  useEffect(() => {
    const handleExpiredSession = () => setSession(null)
    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, handleExpiredSession)
    return () => window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, handleExpiredSession)
  }, [])

  useEffect(() => {
    if (!session) return

    const remainingTime = session.expiresAt - Date.now()
    const timeout = window.setTimeout(
      logout,
      Math.min(Math.max(remainingTime, 0), 2_147_483_647),
    )
    return () => window.clearTimeout(timeout)
  }, [logout, session])

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      isAuthenticated: session !== null,
      login,
      register,
      markOnboardingCompleted,
      logout,
    }),
    [login, logout, markOnboardingCompleted, register, session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
