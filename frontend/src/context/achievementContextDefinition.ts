import { createContext } from 'react'
import type { Achievement, AchievementOverview } from '../types/achievement'

export interface AchievementContextValue {
  overview: AchievementOverview | null
  loading: boolean
  error: boolean
  refreshAchievements: () => Promise<void>
}

export interface AchievementCelebration {
  achievement: Achievement
  id: number
}

export const AchievementContext = createContext<AchievementContextValue | null>(null)
