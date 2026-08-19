import { motion } from 'framer-motion'
import { AchievementIcon } from '../components/achievement/AchievementIcon'
import { AppHeader } from '../components/navigation/AppHeader'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { useAchievements } from '../hooks/useAchievements'
import type { Achievement, AchievementCategory } from '../types/achievement'

const categoryLabels: Record<AchievementCategory, string> = {
  DISCOVERY: 'Descoberta',
  COLLECTION: 'Coleção',
  EXPLORATION: 'Exploração',
  SOCIAL: 'Juntos',
  HABIT: 'Ritual',
}

function AchievementCard({ achievement, index }: { achievement: Achievement; index: number }) {
  const visibleProgress = Math.min(achievement.progress, achievement.target)
  const percentage = Math.round((visibleProgress / achievement.target) * 100)

  return (
    <motion.article
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: 'spring', stiffness: 310, damping: 27, delay: Math.min(index, 8) * 0.04 }}
      className={`relative overflow-hidden rounded-2xl border p-5 ${
        achievement.unlocked
          ? 'border-gold/25 bg-[linear-gradient(145deg,rgba(231,184,98,.1),rgba(255,255,255,.025))]'
          : 'border-white/8 bg-white/[0.025]'
      }`}
    >
      <div className="flex items-start gap-4">
        <div className={`grid size-12 shrink-0 place-items-center rounded-xl border ${
          achievement.unlocked
            ? 'border-gold/25 bg-gold text-canvas shadow-[0_0_28px_rgba(231,184,98,.14)]'
            : 'border-white/8 bg-white/[0.035] text-white/28'
        }`}>
          <AchievementIcon iconKey={achievement.iconKey} />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-3">
            <span className={`text-[10px] font-bold uppercase tracking-[.14em] ${achievement.unlocked ? 'text-gold' : 'text-white/35'}`}>
              {categoryLabels[achievement.category]}
            </span>
            <span className="text-[10px] font-bold tabular-nums text-white/40">
              {visibleProgress}/{achievement.target}
            </span>
          </div>
          <h2 className={`mt-1 text-lg font-extrabold ${achievement.unlocked ? 'text-paper' : 'text-white/58'}`}>
            {achievement.name}
          </h2>
          <p className="mt-1 text-xs leading-5 text-white/48">{achievement.description}</p>
        </div>
      </div>
      <div className="mt-5 h-1.5 overflow-hidden rounded-full bg-white/[0.06]">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${percentage}%` }}
          transition={{ type: 'spring', stiffness: 150, damping: 25, delay: 0.15 + index * 0.02 }}
          className={`h-full rounded-full ${achievement.unlocked ? 'bg-gold' : 'bg-white/25'}`}
        />
      </div>
      {achievement.unlocked ? (
        <span className="mt-3 block text-[10px] font-bold uppercase tracking-wider text-gold/75">
          Desbloqueada
        </span>
      ) : null}
    </motion.article>
  )
}

function AchievementSkeletons() {
  return (
    <div className="grid gap-3 md:grid-cols-2" role="status" aria-label="Carregando conquistas">
      {Array.from({ length: 6 }, (_, index) => (
        <motion.div key={index} animate={{ opacity: [0.18, 0.4, 0.18] }} transition={{ duration: 1.3, delay: index * 0.06, repeat: Infinity }} className="h-44 rounded-2xl bg-white/[0.06]" />
      ))}
    </div>
  )
}

export function AchievementsPage() {
  const { overview, loading, error, refreshAchievements } = useAchievements()
  const percentage = overview && overview.totalCount > 0
    ? Math.round((overview.unlockedCount / overview.totalCount) * 100)
    : 0

  return (
    <main className="relative min-h-svh overflow-hidden bg-canvas px-4 py-5 pb-28 text-paper sm:px-8 sm:py-6 lg:pb-12">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-[32rem] bg-[radial-gradient(circle_at_72%_8%,rgba(231,184,98,.12),transparent_36%)]" />
      <AppHeader />

      <section className="relative mx-auto max-w-5xl pt-10 sm:pt-14">
        <p className="reelz-kicker">Seu caminho no cinema</p>
        <div className="mt-2 grid gap-6 border-b border-white/10 pb-8 md:grid-cols-[1fr_auto] md:items-end">
          <div>
            <h1 className="text-4xl font-extrabold tracking-[-0.045em] sm:text-6xl">Sala de Troféus</h1>
            <p className="mt-3 max-w-xl text-sm leading-6 text-white/58 sm:text-base">
              Marcos de filmes que você guardou, descobriu e escolheu com gente de verdade.
            </p>
          </div>
          {overview ? (
            <div className="flex items-center gap-4 rounded-2xl border border-gold/20 bg-gold/[0.055] px-5 py-4">
              <strong className="text-4xl font-black tabular-nums text-gold">{overview.unlockedCount}</strong>
              <div>
                <p className="text-xs font-bold text-paper">de {overview.totalCount} conquistas</p>
                <p className="mt-1 text-[10px] font-semibold uppercase tracking-wider text-white/40">{percentage}% da sala</p>
              </div>
            </div>
          ) : null}
        </div>

        <div className="mt-7">
          {loading && !overview ? <AchievementSkeletons /> : null}
          {error && !overview ? (
            <div className="rounded-2xl border border-red-300/15 bg-red-300/[0.055] p-6 text-center">
              <p className="text-sm font-semibold text-red-100">Não foi possível abrir seus troféus agora.</p>
              <button type="button" onClick={() => void refreshAchievements()} className="mt-3 text-xs font-bold text-red-100 underline underline-offset-4">Tentar novamente</button>
            </div>
          ) : null}
          {overview ? (
            <div className="grid gap-3 md:grid-cols-2">
              {overview.achievements.map((achievement, index) => (
                <AchievementCard key={achievement.code} achievement={achievement} index={index} />
              ))}
            </div>
          ) : null}
        </div>
      </section>
      <BottomNavigation />
    </main>
  )
}
