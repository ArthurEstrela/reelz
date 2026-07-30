import axios from 'axios'
import type { ApiErrorResponse } from '../types/api'

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) return fallback

  const apiError = error.response?.data
  if (apiError?.violations?.length) {
    return apiError.violations.map((violation) => violation.message).join(' ')
  }
  if (apiError?.message) return apiError.message
  if (error.code === 'ECONNABORTED') {
    return 'A conexão demorou demais. Tente novamente em instantes.'
  }
  if (!error.response) {
    return 'Não foi possível conectar ao Reelz. Verifique sua internet.'
  }
  return fallback
}
