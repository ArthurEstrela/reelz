import { api } from './api'
import type { RouletteSpinRequest, RouletteSpinResponse } from '../types/roulette'

export async function spinRoulette(payload: RouletteSpinRequest): Promise<RouletteSpinResponse> {
  const { data } = await api.post<RouletteSpinResponse>('/api/v1/roulette/spin', payload)
  return data
}
