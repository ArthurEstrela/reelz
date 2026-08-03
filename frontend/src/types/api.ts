export interface FieldViolation {
  field: string
  message: string
}

export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  code: string
  message: string
  path: string
  violations: FieldViolation[]
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresInSeconds: number
  userId: string
  onboardingCompleted: boolean
  role: 'USER' | 'ADMIN'
}

export interface RegisterRequest {
  displayName: string
  email: string
  password: string
  timezone: string
  countryCode: string
  termsAccepted: boolean
}

export interface UserResponse {
  id: string
  displayName: string
  email: string
  plan: 'FREE' | 'PREMIUM'
  role: 'USER' | 'ADMIN'
  timezone: string
  countryCode: string
  onboardingCompleted: boolean
  createdAt: string
}
