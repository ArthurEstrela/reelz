import { AnimatePresence, motion } from 'framer-motion'
import { useCallback, useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react'
import { Link } from 'react-router'
import { AchievementIcon } from '../components/achievement/AchievementIcon'
import { useAuth } from '../hooks/useAuth'
import { getAchievementOverview } from '../services/achievementService'
import type { Achievement, AchievementOverview } from '../types/achievement'
import { AchievementContext, type AchievementContextValue } from './achievementContextDefinition'

export function AchievementProvider({ children }: PropsWithChildren) {
  const { isAuthenticated, user } = useAuth()
  const [overview, setOverview] = useState<AchievementOverview | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)
  const [celebrations, setCelebrations] = useState<Achievement[]>([])
  const baselineReady = useRef(false)
  const unlockedCodes = useRef<Set<string>>(new Set())

  const load = useCallback(async (notify: boolean) => {
    if (!isAuthenticated) return
    setLoading(true)
    try {
      const nextOverview = await getAchievementOverview()
      const nextUnlocked = nextOverview.achievements.filter((item) => item.unlocked)
      if (notify && baselineReady.current) {
        const newlyUnlocked = nextUnlocked.filter((item) => !unlockedCodes.current.has(item.code))
        if (newlyUnlocked.length > 0) {
          setCelebrations((current) => [
            ...current,
            ...newlyUnlocked.filter((item) => !current.some((queued) => queued.code === item.code)),
          ])
        }
      }
      unlockedCodes.current = new Set(nextUnlocked.map((item) => item.code))
      baselineReady.current = true
      setOverview(nextOverview)
      setError(false)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated])

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      baselineReady.current = false
      unlockedCodes.current = new Set()
      setOverview(null)
      setCelebrations([])
      if (isAuthenticated) void load(false)
    }, 0)
    return () => window.clearTimeout(timeout)
  }, [isAuthenticated, load, user?.id])

  useEffect(() => {
    if (celebrations.length === 0) return
    const timeout = window.setTimeout(() => {
      setCelebrations((current) => current.slice(1))
    }, 5_500)
    return () => window.clearTimeout(timeout)
  }, [celebrations])

  const celebration = celebrations[0] ?? null

  const value = useMemo<AchievementContextValue>(() => ({
    overview,
    loading,
    error,
    refreshAchievements: () => load(true),
  }), [error, load, loading, overview])

  return (
    <AchievementContext.Provider value={value}>
      {children}
      <AnimatePresence>
        {celebration ? (
          <motion.aside
            role="status"
            initial={{ opacity: 0, y: 38, scale: 0.88 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 18, scale: 0.96 }}
            transition={{ type: 'spring', stiffness: 390, damping: 24, bounce: 0.28 }}
            className="fixed inset-x-4 bottom-24 z-[70] mx-auto flex max-w-md items-center gap-4 rounded-2xl border border-gold/30 bg-[#211b12]/95 p-4 text-paper shadow-[0_24px_80px_rgba(0,0,0,.55)] backdrop-blur-xl lg:bottom-7"
          >
            <div className="grid size-12 shrink-0 place-items-center rounded-xl bg-gold text-canvas shadow-[0_0_30px_rgba(231,184,98,.2)]">
              <AchievementIcon iconKey={celebration.iconKey} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-[10px] font-bold uppercase tracking-[.16em] text-gold">Conquista desbloqueada</p>
              <p className="mt-1 truncate text-base font-extrabold">{celebration.name}</p>
              <p className="mt-0.5 text-xs text-white/60">{celebration.description}</p>
            </div>
            <Link to="/achievements" onClick={() => setCelebrations((current) => current.slice(1))} className="shrink-0 text-xs font-bold text-gold hover:text-white">
              Ver
            </Link>
          </motion.aside>
        ) : null}
      </AnimatePresence>
    </AchievementContext.Provider>
  )
}
