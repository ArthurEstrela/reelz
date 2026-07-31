import { createContext } from 'react'
import type { RegisterRequest } from '../types/api'
import type { AuthUser } from '../types/auth'

export interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (payload: RegisterRequest) => Promise<void>
  markOnboardingCompleted: () => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
