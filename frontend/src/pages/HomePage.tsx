import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import axios from 'axios'
import { AnimatePresence, MotionConfig, motion } from 'framer-motion'
import { ReelzLogo } from '../components/brand/ReelzLogo'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { FilterPills, type PillOption } from '../components/roulette/FilterPills'
import { MovieCard } from '../components/roulette/MovieCard'
import { SlotMachine } from '../components/roulette/SlotMachine'
import { SpinLimitModal } from '../components/roulette/SpinLimitModal'
import { GENRE_OPTIONS } from '../config/rouletteFilters'
import { useAuth } from '../hooks/useAuth'
import { getProviders, getVibes } from '../services/catalogService'
import { markMovieAsWatched } from '../services/historyService'
import { getTodayUsage, spinRoulette } from '../services/rouletteService'
import type { CatalogItem } from '../types/catalog'
import type { RouletteMovie, SpinQuota } from '../types/roulette'
import { getApiErrorMessage } from '../utils/apiError'

type RouletteState = 'idle' | 'spinning' | 'result' | 'empty'
type CatalogState = 'loading' | 'ready' | 'error'

interface HomePageProps {
  minimumSpinDuration?: number
}

interface ToastMessage {
  id: number
  message: string
}

function toPillOptions(items: CatalogItem[]): PillOption<string>[] {
  return items.map((item) => ({ value: item.id, label: item.name }))
}

async function waitForMinimumDuration(startedAt: number, minimumDuration: number) {
  const remaining = Math.max(0, minimumDuration - (performance.now() - startedAt))
  if (remaining > 0) await new Promise((resolve) => window.setTimeout(resolve, remaining))
}

export function HomePage({ minimumSpinDuration = 2_000 }: HomePageProps) {
  const { user, logout } = useAuth()
  const quotaRequestSequence = useRef(0)
  const [providerOptions, setProviderOptions] = useState<PillOption<string>[]>([])
  const [vibeOptions, setVibeOptions] = useState<PillOption<string>[]>([])
  const [catalogState, setCatalogState] = useState<CatalogState>('loading')
  const [catalogReloadKey, setCatalogReloadKey] = useState(0)
  const [selectedProviders, setSelectedProviders] = useState<string[]>([])
  const [selectedGenre, setSelectedGenre] = useState<number | null>(null)
  const [selectedVibe, setSelectedVibe] = useState<string | null>(null)
  const [rouletteState, setRouletteState] = useState<RouletteState>('idle')
  const [movie, setMovie] = useState<RouletteMovie | null>(null)
  const [quota, setQuota] = useState<SpinQuota | null>(null)
  const [quotaSyncFailed, setQuotaSyncFailed] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [failureKey, setFailureKey] = useState(0)
  const [showLimitModal, setShowLimitModal] = useState(false)
  const [toast, setToast] = useState<ToastMessage | null>(null)

  const synchronizeQuota = useCallback(async () => {
    const requestSequence = ++quotaRequestSequence.current
    try {
      const currentQuota = await getTodayUsage()
      if (requestSequence !== quotaRequestSequence.current) return
      setQuota(currentQuota)
      setQuotaSyncFailed(false)
    } catch {
      if (requestSequence === quotaRequestSequence.current) setQuotaSyncFailed(true)
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    Promise.all([getProviders(), getVibes()])
      .then(([providers, vibes]) => {
        if (cancelled) return
        const nextProviderOptions = toPillOptions(providers)
        const nextVibeOptions = toPillOptions(vibes)
        const validProviderIds = new Set(providers.map((provider) => provider.id))
        const validVibeIds = new Set(vibes.map((vibe) => vibe.id))

        setProviderOptions(nextProviderOptions)
        setVibeOptions(nextVibeOptions)
        setSelectedProviders((current) => {
          const validSelection = current.filter((providerId) => validProviderIds.has(providerId))
          if (validSelection.length > 0) return validSelection
          return providers[0] ? [providers[0].id] : []
        })
        setSelectedVibe((current) => current && validVibeIds.has(current) ? current : null)
        setCatalogState('ready')
      })
      .catch(() => {
        if (!cancelled) setCatalogState('error')
      })

    return () => {
      cancelled = true
    }
  }, [catalogReloadKey])

  useEffect(() => {
    const timeout = window.setTimeout(() => void synchronizeQuota(), 0)
    return () => window.clearTimeout(timeout)
  }, [synchronizeQuota])

  useEffect(() => {
    if (!toast) return
    const timeout = window.setTimeout(() => setToast(null), 4_500)
    return () => window.clearTimeout(timeout)
  }, [toast])

  const remainingSpins = useMemo(() => {
    if (!quota) return quotaSyncFailed ? '?' : '…'
    if (quota.unlimited) return '∞'
    return String(Math.max(0, (quota.remainingDailySpins ?? 0) + quota.remainingRewardedSpins))
  }, [quota, quotaSyncFailed])

  const quotaAriaLabel = quota
    ? `${remainingSpins} giros restantes hoje`
    : quotaSyncFailed
      ? 'Não foi possível sincronizar os giros restantes hoje'
      : 'Carregando giros restantes hoje'
  const isSpinning = rouletteState === 'spinning'
  const catalogLoading = catalogState === 'loading'
  const hasProviders = providerOptions.length > 0

  function toggleProvider(providerId: string) {
    setSelectedProviders((current) => {
      if (current.includes(providerId)) return current.filter((id) => id !== providerId)
      return quota?.unlimited ? [...current, providerId] : [providerId]
    })
  }

  function toggleGenre(genreId: number) {
    setSelectedGenre((current) => (current === genreId ? null : genreId))
  }

  function toggleVibe(vibeId: string) {
    setSelectedVibe((current) => (current === vibeId ? null : vibeId))
  }

  function showSpinFailure(nextMessage: string) {
    setMessage(nextMessage)
    setMovie(null)
    setRouletteState('empty')
    setFailureKey((current) => current + 1)
  }

  async function executeSpin(startedAt = performance.now()) {
    setMessage(null)
    setMovie(null)
    setRouletteState('spinning')

    try {
      const response = await spinRoulette({
        idempotencyKey: crypto.randomUUID(),
        providerIds: selectedProviders,
        genreId: selectedGenre,
        vibeId: selectedVibe,
      })
      setQuota(response.quota)
      void synchronizeQuota()
      await waitForMinimumDuration(startedAt, minimumSpinDuration)
      setMovie(response.movie)
      setRouletteState('result')
    } catch (error) {
      await waitForMinimumDuration(startedAt, minimumSpinDuration)
      const status = axios.isAxiosError(error) ? error.response?.status : undefined

      if (status === 404) {
        showSpinFailure('A roleta procurou até debaixo do sofá e não achou nada. Que tal mudar os filtros?')
        return
      }

      if (status === 429 || status === 403) {
        setQuota((current) => ({
          unlimited: current?.unlimited ?? false,
          dailyLimit: current?.dailyLimit ?? 5,
          remainingDailySpins: 0,
          remainingRewardedSpins: 0,
        }))
        setRouletteState('idle')
        setShowLimitModal(true)
        return
      }

      showSpinFailure(getApiErrorMessage(error, 'A projeção falhou por um instante. Tente girar novamente.'))
    }
  }

  function handleSpin() {
    if (isSpinning) return
    if (selectedProviders.length === 0) {
      showSpinFailure('Escolha pelo menos um streaming antes de girar. A roleta também tem seus limites!')
      return
    }
    void executeSpin()
  }

  function retryCatalog() {
    setCatalogState('loading')
    setCatalogReloadKey((key) => key + 1)
  }

  async function handleWatchedAndSpinAgain() {
    if (!movie || isSpinning) return
    const watchedMovie = movie
    const animationStartedAt = performance.now()

    setMessage(null)
    setMovie(null)
    setRouletteState('spinning')

    try {
      await markMovieAsWatched(watchedMovie.tmdbId)
    } catch (error) {
      setToast({
        id: Date.now(),
        message: getApiErrorMessage(
          error,
          `Não conseguimos marcar “${watchedMovie.title}” como visto. Tente novamente depois.`,
        ),
      })
    }

    void executeSpin(animationStartedAt)
  }

  return (
    <MotionConfig reducedMotion="user">
      <main className="relative min-h-svh overflow-hidden bg-canvas px-4 pt-5 pb-28 text-white sm:px-8 sm:pt-6 sm:pb-28">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_24%,rgba(255,60,72,.16),transparent_30%)]" />

        <header className="relative mx-auto flex max-w-5xl items-center justify-between gap-3">
          <ReelzLogo />
          <div className="flex items-center gap-2">
            <div
              className="rounded-full border border-white/10 bg-white/[0.045] px-3 py-2 text-right"
              title={quotaSyncFailed ? 'Não foi possível atualizar a franquia.' : undefined}
            >
              <span className="block text-[9px] font-extrabold uppercase tracking-[0.14em] text-white/35">Giros hoje</span>
              <span className="block text-sm font-black leading-none text-white" aria-label={quotaAriaLabel}>
                {remainingSpins}
              </span>
            </div>
            <button
              type="button"
              onClick={logout}
              title={user?.email ? `Sair de ${user.email}` : 'Sair'}
              className="rounded-xl border border-white/10 px-3 py-2.5 text-xs font-bold text-white/50 transition hover:border-white/20 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-reel"
            >
              Sair
            </button>
          </div>
        </header>

        <div className="relative mx-auto flex w-full max-w-3xl flex-col pb-12 pt-8 sm:pt-12">
          <section className="flex min-h-[22rem] items-center justify-center text-center sm:min-h-[27rem]" aria-label="Roleta de filmes">
            <AnimatePresence mode="wait">
              {rouletteState === 'idle' ? (
                <motion.div
                  key="idle"
                  initial={{ opacity: 0, scale: 0.7, y: 35 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.75, rotate: 8 }}
                  transition={{ type: 'spring', bounce: 0.44, duration: 0.72 }}
                  className="flex flex-col items-center"
                >
                  <motion.div
                    animate={{ y: [0, -8, 0], rotate: [0, -2, 2, 0] }}
                    transition={{ duration: 3.4, repeat: Infinity, ease: 'easeInOut' }}
                    className="relative grid size-40 place-items-center sm:size-48"
                    aria-hidden="true"
                  >
                    <div className="absolute inset-0 rounded-full border border-dashed border-white/15" />
                    <div className="absolute inset-5 rounded-full border border-white/10 bg-white/[0.025] shadow-[0_25px_90px_rgba(255,60,72,.22)]" />
                    <div className="absolute inset-10 rounded-full bg-gradient-to-br from-reel-bright to-red-800" />
                    <svg viewBox="0 0 24 24" className="relative ml-1 size-10 text-white">
                      <path fill="currentColor" d="M8 6.7c0-1.2 1.3-1.9 2.3-1.3l7 4.1a1.7 1.7 0 0 1 0 2.9l-7 4.2A1.5 1.5 0 0 1 8 15.3V6.7Z" />
                    </svg>
                  </motion.div>
                  <p className="mt-7 text-xs font-black uppercase tracking-[0.24em] text-reel">Sua próxima sessão</p>
                  <h1 className="mt-2 text-4xl font-black tracking-[-0.055em] sm:text-5xl">A um giro de distância</h1>
                </motion.div>
              ) : null}

              {rouletteState === 'spinning' ? <SlotMachine key="spinning" /> : null}

              {rouletteState === 'result' && movie ? (
                <MovieCard
                  key={movie.id}
                  movie={movie}
                  onWatchedAndSpinAgain={handleWatchedAndSpinAgain}
                  spinning={isSpinning}
                />
              ) : null}

              {rouletteState === 'empty' ? (
                <motion.div
                  key={`empty-${failureKey}`}
                  initial={{ opacity: 0, scale: 0.88 }}
                  animate={{ opacity: 1, scale: 1, x: [0, -15, 13, -10, 7, 0] }}
                  exit={{ opacity: 0, scale: 0.85 }}
                  transition={{ type: 'spring', stiffness: 430, damping: 20 }}
                  className="max-w-sm rounded-[2rem] border border-amber-300/15 bg-amber-300/[0.06] p-7"
                  role="alert"
                >
                  <span className="text-4xl" aria-hidden="true">🫨</span>
                  <p className="mt-4 text-base font-bold leading-7 text-white/75">{message}</p>
                </motion.div>
              ) : null}
            </AnimatePresence>
          </section>

          {rouletteState !== 'result' ? (
            <motion.button
              type="button"
              onClick={handleSpin}
              disabled={isSpinning || catalogLoading || !hasProviders}
              whileTap={{ scale: 0.95 }}
              whileHover={isSpinning ? undefined : { scale: 1.015 }}
              transition={{ type: 'spring', stiffness: 380, damping: 22 }}
              className="mx-auto w-full max-w-md rounded-[1.4rem] bg-reel px-7 py-5 text-lg font-black text-white shadow-[0_18px_55px_rgba(255,60,72,.3)] transition-colors hover:bg-reel-bright disabled:cursor-not-allowed disabled:bg-white/10 disabled:text-white/30 disabled:shadow-none focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-reel"
            >
              {isSpinning ? 'Girando…' : catalogLoading ? 'Carregando catálogo…' : 'Girar Roleta'}
            </motion.button>
          ) : null}

          {catalogState === 'error' ? (
            <div className="mx-auto mt-3 flex max-w-md items-center gap-2 text-center text-xs text-amber-200/70" role="alert">
              <span>Não foi possível carregar os filtros.</span>
              <button type="button" onClick={retryCatalog} className="font-black underline underline-offset-2">
                Tentar novamente
              </button>
            </div>
          ) : null}

          {catalogState === 'ready' && !hasProviders ? (
            <p className="mx-auto mt-3 max-w-md text-center text-xs leading-5 text-amber-200/65">
              Nenhum streaming está ativo no catálogo no momento.
            </p>
          ) : null}

          <section className="mt-9 space-y-6 rounded-[1.75rem] border border-white/8 bg-white/[0.025] p-4 sm:p-6" aria-label="Filtros rápidos">
            <FilterPills
              legend={quota?.unlimited ? 'Onde assistir · escolha vários' : 'Onde assistir · 1 por vez'}
              options={providerOptions}
              selectedValues={selectedProviders}
              onToggle={toggleProvider}
              loading={catalogLoading}
              disabled={isSpinning}
            />
            <FilterPills
              legend="Gênero · opcional"
              options={GENRE_OPTIONS}
              selectedValues={selectedGenre === null ? [] : [selectedGenre]}
              onToggle={toggleGenre}
              disabled={isSpinning}
            />
            <FilterPills
              legend="Vibe · opcional"
              options={vibeOptions}
              selectedValues={selectedVibe === null ? [] : [selectedVibe]}
              onToggle={toggleVibe}
              loading={catalogLoading}
              disabled={isSpinning}
            />
          </section>
        </div>

        <AnimatePresence>
          {showLimitModal ? <SpinLimitModal key="limit-modal" onClose={() => setShowLimitModal(false)} /> : null}
          {toast ? (
            <motion.div
              key={toast.id}
              role="alert"
              initial={{ opacity: 0, y: 28, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 18, scale: 0.94 }}
              transition={{ type: 'spring', stiffness: 420, damping: 28 }}
              className="fixed inset-x-4 bottom-24 z-50 mx-auto max-w-md rounded-2xl border border-red-300/15 bg-[#241114]/95 px-4 py-3 text-sm font-bold text-red-100 shadow-2xl backdrop-blur"
            >
              {toast.message}
            </motion.div>
          ) : null}
        </AnimatePresence>
        <BottomNavigation />
      </main>
    </MotionConfig>
  )
}
