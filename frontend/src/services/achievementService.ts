import { api } from './api'
import type { AchievementOverview } from '../types/achievement'

export async function getAchievementOverview(): Promise<AchievementOverview> {
  const { data } = await api.get<AchievementOverview>('/api/v1/achievements')
  return data
}
