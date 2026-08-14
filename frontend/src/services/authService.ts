import { api } from './api'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserResponse,
  UpdateUserRequest,
} from '../types/api'

export async function loginRequest(payload: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/api/v1/auth/login', payload)
  return data
}

export async function registerRequest(payload: RegisterRequest): Promise<UserResponse> {
  const { data } = await api.post<UserResponse>('/api/v1/users', payload)
  return data
}

export async function requestEmailVerification(email: string): Promise<void> {
  await api.post('/api/v1/auth/email-verification/request', { email })
}

export async function confirmEmailVerification(token: string): Promise<void> {
  await api.post('/api/v1/auth/email-verification/confirm', { token })
}

export async function requestPasswordReset(email: string): Promise<void> {
  await api.post('/api/v1/auth/password-reset/request', { email })
}

export async function confirmPasswordReset(token: string, newPassword: string): Promise<void> {
  await api.post('/api/v1/auth/password-reset/confirm', { token, newPassword })
}

export async function getCurrentUser(): Promise<UserResponse> {
  const { data } = await api.get<UserResponse>('/api/v1/users/me')
  return data
}

export async function updateCurrentUser(payload: UpdateUserRequest): Promise<UserResponse> {
  const { data } = await api.patch<UserResponse>('/api/v1/users/me', payload)
  return data
}

export async function deleteCurrentUser(password: string): Promise<void> {
  await api.delete('/api/v1/users/me', { data: { password, confirmed: true } })
}
