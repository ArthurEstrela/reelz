import { useContext } from 'react'
import { AchievementContext } from '../context/achievementContextDefinition'

export function useAchievements() {
  const context = useContext(AchievementContext)
  if (!context) throw new Error('useAchievements must be used within an AchievementProvider')
  return context
}
