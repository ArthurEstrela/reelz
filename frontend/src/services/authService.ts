import { api } from './api'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserResponse,
} from '../types/api'

export async function loginRequest(payload: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/api/v1/auth/login', payload)
  return data
}

export async function registerRequest(payload: RegisterRequest): Promise<UserResponse> {
  const { data } = await api.post<UserResponse>('/api/v1/users', payload)
  return data
}
