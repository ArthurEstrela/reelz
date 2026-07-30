import axios from 'axios'
import {
  AUTH_SESSION_EXPIRED_EVENT,
  clearAuthSession,
  getAuthSession,
} from '../storage/authStorage'

const baseURL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

export const api = axios.create({
  baseURL,
  timeout: 10_000,
  headers: {
    Accept: 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const session = getAuthSession()
  if (session) {
    config.headers.set('Authorization', `Bearer ${session.accessToken}`)
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearAuthSession()
      window.dispatchEvent(new Event(AUTH_SESSION_EXPIRED_EVENT))
    }
    return Promise.reject(error)
  },
)
