export interface AuthUser {
  id: string
  email: string
  onboardingCompleted: boolean
}

export interface AuthSession {
  accessToken: string
  expiresAt: number
  user: AuthUser
}
