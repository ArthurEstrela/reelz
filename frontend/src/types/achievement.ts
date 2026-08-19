export type AchievementCategory = 'DISCOVERY' | 'COLLECTION' | 'EXPLORATION' | 'SOCIAL' | 'HABIT'

export type AchievementCode =
  | 'FIRST_SPIN'
  | 'OPEN_PROVIDER'
  | 'WATCHED_10'
  | 'WATCHED_50'
  | 'WATCHED_100'
  | 'WATCHLIST_5'
  | 'GENRES_5'
  | 'COUPLE_SPIN'
  | 'GROUP_SPIN_3'
  | 'ACTIVE_WEEKS_4'

export interface Achievement {
  code: AchievementCode
  name: string
  description: string
  iconKey: string
  category: AchievementCategory
  target: number
  progress: number
  unlocked: boolean
  unlockedAt: string | null
}

export interface AchievementOverview {
  unlockedCount: number
  totalCount: number
  achievements: Achievement[]
}
