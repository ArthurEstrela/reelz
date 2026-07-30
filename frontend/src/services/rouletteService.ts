import { api } from './api'
import type { RouletteSpinRequest, RouletteSpinResponse, SpinQuota } from '../types/roulette'

export async function spinRoulette(payload: RouletteSpinRequest): Promise<RouletteSpinResponse> {
  const { data } = await api.post<RouletteSpinResponse>('/api/v1/roulette/spin', payload)
  return data
}

export async function getTodayUsage(): Promise<SpinQuota> {
  const { data } = await api.get<SpinQuota>('/api/v1/roulette/usage/today')
  return data
}
