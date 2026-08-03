export interface AuthUser {
  id: string
  email: string
  onboardingCompleted: boolean
  role: 'USER' | 'ADMIN'
}

export interface AuthSession {
  accessToken: string
  expiresAt: number
  user: AuthUser
}
