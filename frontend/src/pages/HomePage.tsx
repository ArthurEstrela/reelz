import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import axios from 'axios'
import { AnimatePresence, MotionConfig, motion } from 'framer-motion'
import { Link } from 'react-router'
import { getProductSessionId } from '../analytics/productSession'
import { AppHeader } from '../components/navigation/AppHeader'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { FilterPills, type PillOption } from '../components/roulette/FilterPills'
import { MovieCard } from '../components/roulette/MovieCard'
import { SlotMachine } from '../components/roulette/SlotMachine'
import { SpinLimitModal } from '../components/roulette/SpinLimitModal'
import { PWA_ENGAGEMENT_EVENT } from '../components/pwa/PwaStatusPrompt'
import { StreamingPreferencesModal } from '../components/streaming/StreamingPreferencesModal'
import { GENRE_OPTIONS } from '../config/rouletteFilters'
import { trackProductEventInBackground } from '../services/analyticsService'
import { getProviders, getVibes } from '../services/catalogService'
import {
  markMovieAsWatched,
  saveMovieToWatchlist,
} from '../services/historyService'
import { getTodayUsage, spinRoulette } from '../services/rouletteService'
import {
  getStreamingPreferences,
  updateStreamingPreferences,
} from '../services/streamingPreferenceService'
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
  tone?: 'error' | 'success' | 'info'
}

function toPillOptions(items: CatalogItem[]): PillOption<string>[] {
  return items.map((item) => ({ value: item.id, label: item.name }))
}

async function waitForMinimumDuration(startedAt: number, minimumDuration: number) {
  const remaining = Math.max(0, minimumDuration - (performance.now() - startedAt))
  if (remaining > 0) await new Promise((resolve) => window.setTimeout(resolve, remaining))
}

export function HomePage({ minimumSpinDuration = 2_000 }: HomePageProps) {
  const productSessionId = useMemo(() => getProductSessionId(), [])
  const quotaRequestSequence = useRef(0)
  const [providers, setProviders] = useState<CatalogItem[]>([])
  const [vibeOptions, setVibeOptions] = useState<PillOption<string>[]>([])
  const [catalogState, setCatalogState] = useState<CatalogState>('loading')
  const [catalogReloadKey, setCatalogReloadKey] = useState(0)
  const [selectedProviders, setSelectedProviders] = useState<string[]>([])
  const [ownedProviderIds, setOwnedProviderIds] = useState<string[]>([])
  const [showStreamingPreferences, setShowStreamingPreferences] = useState(false)
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

  const allProviderOptions = useMemo(() => toPillOptions(providers), [providers])
  const providerOptions = useMemo(() => {
    if (ownedProviderIds.length === 0) return allProviderOptions
    const ownedIds = new Set(ownedProviderIds)
    return allProviderOptions.filter((provider) => ownedIds.has(provider.value))
  }, [allProviderOptions, ownedProviderIds])

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
    let preferencesFailed = false

    Promise.all([
      getProviders(),
      getVibes(),
      getStreamingPreferences().catch(() => {
        preferencesFailed = true
        return { providerIds: [] }
      }),
    ])
      .then(([providerCatalog, vibes, preferences]) => {
        if (cancelled) return
        const nextVibeOptions = toPillOptions(vibes)
        const validProviderIds = new Set(providerCatalog.map((provider) => provider.id))
        const validVibeIds = new Set(vibes.map((vibe) => vibe.id))
        const validOwnedProviderIds = preferences.providerIds.filter((providerId) => validProviderIds.has(providerId))
        const initialProviderId = validOwnedProviderIds[0] ?? providerCatalog[0]?.id

        setProviders(providerCatalog)
        setVibeOptions(nextVibeOptions)
        setOwnedProviderIds(validOwnedProviderIds)
        setSelectedProviders((current) => {
          const selectableProviderIds = validOwnedProviderIds.length > 0
            ? new Set(validOwnedProviderIds)
            : validProviderIds
          const validSelection = current.filter((providerId) => selectableProviderIds.has(providerId))
          if (validSelection.length > 0) return validSelection
          return initialProviderId ? [initialProviderId] : []
        })
        setSelectedVibe((current) => current && validVibeIds.has(current) ? current : null)
        setCatalogState('ready')
        if (preferencesFailed) {
          setToast({
            id: Date.now(),
            message: 'O catálogo carregou, mas não conseguimos recuperar seus streamings salvos.',
          })
        }
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
    trackProductEventInBackground('HOME_VIEWED')
  }, [])

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
        sessionId: productSessionId,
      })
      setQuota(response.quota)
      void synchronizeQuota()
      await waitForMinimumDuration(startedAt, minimumSpinDuration)
      setMovie(response.movie)
      setRouletteState('result')
      window.dispatchEvent(new Event(PWA_ENGAGEMENT_EVENT))
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

  async function saveStreamingPreferences(providerIds: string[]) {
    const savedPreferences = await updateStreamingPreferences({ providerIds })
    setOwnedProviderIds(savedPreferences.providerIds)
    setSelectedProviders((current) => {
      const availableIds = savedPreferences.providerIds.length > 0
        ? new Set(savedPreferences.providerIds)
        : new Set(providers.map((provider) => provider.id))
      const validSelection = current.filter((providerId) => availableIds.has(providerId))
      if (validSelection.length > 0) return quota?.unlimited ? validSelection : [validSelection[0]]
      const firstProviderId = savedPreferences.providerIds[0] ?? providers[0]?.id
      return firstProviderId ? [firstProviderId] : []
    })
    setShowStreamingPreferences(false)
    setToast({
      id: Date.now(),
      message: savedPreferences.providerIds.length > 0
        ? 'Seus streamings foram salvos.'
        : 'Preferências limpas. Todos os streamings voltaram a aparecer.',
      tone: 'success',
    })
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

  async function handleSaveToWatchlist(): Promise<boolean> {
    if (!movie || isSpinning) return false
    const movieToSave = movie

    try {
      await saveMovieToWatchlist(movieToSave.tmdbId)
      return true
    } catch (error) {
      setToast({
        id: Date.now(),
        message: getApiErrorMessage(
          error,
          `Não conseguimos salvar “${movieToSave.title}” na lista Quero Ver.`,
        ),
      })
      return false
    }
  }

  function handleSpinAgain() {
    if (isSpinning) return
    void executeSpin()
  }

  function handleWatchProvider() {
    if (!movie?.streamingAvailability[0]) return
    trackProductEventInBackground('WATCH_PROVIDER_CLICKED', {
      movieId: movie.tmdbId,
      providerId: movie.streamingAvailability[0].providerId,
    })
  }

  return (
    <MotionConfig reducedMotion="user">
      <main className="relative min-h-svh overflow-hidden bg-canvas px-4 pt-5 pb-28 text-paper sm:px-8 sm:pt-6 lg:pb-12">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-[34rem] bg-[radial-gradient(circle_at_35%_24%,rgba(233,54,69,.13),transparent_38%)]" />

        <AppHeader
          accessory={
            <div
              className="rounded-full border border-white/10 bg-white/[0.045] px-3 py-2 text-right"
              title={quotaSyncFailed ? 'Não foi possível atualizar a franquia.' : undefined}
            >
              <span className="block text-[0.58rem] font-semibold uppercase tracking-[0.12em] text-white/55">Giros hoje</span>
              <span className="block text-sm font-bold leading-none text-paper" aria-label={quotaAriaLabel}>
                {remainingSpins}
              </span>
            </div>
          }
        />

        <div className="relative mx-auto grid w-full max-w-6xl grid-cols-[minmax(0,1fr)] gap-8 pb-12 pt-9 lg:grid-cols-[minmax(0,1fr)_25rem] lg:items-start lg:gap-12 lg:pt-16">
          <div className="min-w-0">
          <section className="flex min-h-[20rem] items-center justify-center text-center lg:min-h-[30rem]" aria-label="Roleta de filmes">
            <AnimatePresence mode="wait">
              {rouletteState === 'idle' ? (
                <motion.div
                  key="idle"
                  initial={{ opacity: 0, scale: 0.7, y: 35 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.75, rotate: 8 }}
                  transition={{ type: 'spring', bounce: 0.44, duration: 0.72 }}
                  className="flex w-full flex-col items-center"
                >
                  <p className="reelz-kicker">Sua próxima sessão</p>
                  <h1 className="mt-2 text-4xl font-extrabold tracking-[-0.045em] text-paper sm:text-5xl lg:text-6xl">
                    Hoje vai de quê?
                  </h1>
                  <p className="mt-3 max-w-md text-sm leading-6 text-white/55">
                    Um giro, um filme disponível nos streamings que você já tem.
                  </p>
                  <motion.button
                    type="button"
                    onClick={handleSpin}
                    disabled={catalogLoading || !hasProviders}
                    aria-label={catalogLoading ? 'Carregando catálogo…' : 'Girar Roleta'}
                    whileTap={{ scale: 0.94 }}
                    whileHover={{ scale: 1.025 }}
                    animate={{ y: [0, -5, 0] }}
                    transition={{ y: { duration: 3.2, repeat: Infinity, ease: 'easeInOut' }, scale: { type: 'spring', stiffness: 360, damping: 22 } }}
                    className="group relative mt-8 grid size-36 place-items-center rounded-full disabled:cursor-not-allowed disabled:opacity-45 sm:size-40"
                  >
                    <span className="absolute inset-0 rounded-full border border-dashed border-white/20 transition-transform duration-500 group-hover:rotate-12" />
                    <span className="absolute inset-3 rounded-full border border-white/12 bg-surface shadow-[0_20px_70px_rgba(233,54,69,.2)]" />
                    <span className="absolute inset-7 rounded-full bg-reel transition-colors group-hover:bg-reel-bright" />
                    <svg viewBox="0 0 24 24" className="relative ml-1 size-9 text-white" aria-hidden="true">
                      <path fill="currentColor" d="M8 6.7c0-1.2 1.3-1.9 2.3-1.3l7 4.1a1.7 1.7 0 0 1 0 2.9l-7 4.2A1.5 1.5 0 0 1 8 15.3V6.7Z" />
                    </svg>
                    <span className="absolute -bottom-7 whitespace-nowrap text-sm font-bold text-paper">Girar roleta</span>
                  </motion.button>
                </motion.div>
              ) : null}

              {rouletteState === 'spinning' ? <SlotMachine key="spinning" /> : null}

              {rouletteState === 'result' && movie ? (
                <MovieCard
                  key={movie.id}
                  movie={movie}
                  onWatchedAndSpinAgain={handleWatchedAndSpinAgain}
                  onSaveToWatchlist={handleSaveToWatchlist}
                  onSpinAgain={handleSpinAgain}
                  onWatchProvider={handleWatchProvider}
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
                  className="max-w-sm rounded-2xl border border-gold/20 bg-gold/[0.06] p-7"
                  role="alert"
                >
                  <svg viewBox="0 0 24 24" className="mx-auto size-10 text-gold" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
                    <circle cx="12" cy="12" r="9" /><path d="M8.5 9h.01M15.5 9h.01M8.5 16c1.6-2 5.4-2 7 0" />
                  </svg>
                  <p className="mt-4 text-base font-semibold leading-7 text-white/80">{message}</p>
                </motion.div>
              ) : null}
            </AnimatePresence>
          </section>

          {rouletteState === 'spinning' ? (
            <p className="mt-2 text-center text-xs font-medium text-white/50">Isso leva só alguns segundos.</p>
          ) : null}

          </div>

          <aside className="reelz-surface min-w-0 overflow-hidden rounded-2xl p-4 sm:p-5 lg:sticky lg:top-24" aria-label="Filtros rápidos">
            <div className="flex items-start justify-between gap-3 border-b border-white/8 pb-4">
              <div>
                <p className="text-sm font-semibold text-paper">Ajuste o giro</p>
                <span className="mt-1 block text-xs text-white/50">
                  {ownedProviderIds.length > 0
                    ? `${ownedProviderIds.length} streaming${ownedProviderIds.length === 1 ? '' : 's'} salvo${ownedProviderIds.length === 1 ? '' : 's'}`
                    : 'Mostrando todos os streamings'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setShowStreamingPreferences(true)}
                disabled={catalogLoading || isSpinning}
                className="rounded-xl border border-white/12 px-3 py-2 text-xs font-semibold text-white/70 transition hover:border-white/25 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
              >
                {ownedProviderIds.length > 0 ? 'Gerenciar' : 'Meus streamings'}
              </button>
            </div>
            <div className="mt-5 space-y-5">
              <FilterPills
                legend={quota?.unlimited ? 'Streamings · escolha vários' : 'Streaming · 1 por giro'}
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
                legend="Clima · opcional"
                options={vibeOptions}
                selectedValues={selectedVibe === null ? [] : [selectedVibe]}
                onToggle={toggleVibe}
                loading={catalogLoading}
                disabled={isSpinning}
              />
            </div>
            {catalogState === 'error' ? (
              <div className="mt-4 flex items-center gap-2 text-xs text-gold" role="alert">
                <span>Não foi possível carregar os filtros.</span>
                <button type="button" onClick={retryCatalog} className="font-semibold underline underline-offset-2">Tentar novamente</button>
              </div>
            ) : null}
            {catalogState === 'ready' && !hasProviders ? (
              <p className="mt-4 text-xs leading-5 text-gold">Nenhum streaming está ativo no catálogo no momento.</p>
            ) : null}
            <Link
              to="/social"
              onClick={() => trackProductEventInBackground('GROUP_MODE_INTERESTED')}
              className="mt-5 flex items-center justify-between border-t border-white/8 pt-4 text-sm font-semibold text-white/65 transition hover:text-paper"
            >
              Escolhendo com alguém?
              <span aria-hidden="true">Abrir Juntos →</span>
            </Link>
          </aside>

          <footer className="flex justify-center gap-5 text-[11px] font-medium text-white/50 lg:col-span-2">
            <Link to="/about" className="transition hover:text-white">Sobre e créditos</Link>
            <Link to="/privacy" className="transition hover:text-white">Privacidade</Link>
            <Link to="/terms" className="transition hover:text-white">Termos</Link>
          </footer>
        </div>

        <AnimatePresence>
          {showLimitModal ? <SpinLimitModal key="limit-modal" onClose={() => setShowLimitModal(false)} /> : null}
          {showStreamingPreferences ? (
            <StreamingPreferencesModal
              key="streaming-preferences"
              providers={providers}
              selectedProviderIds={ownedProviderIds}
              onClose={() => setShowStreamingPreferences(false)}
              onSave={saveStreamingPreferences}
            />
          ) : null}
          {toast ? (
            <motion.div
              key={toast.id}
              role="alert"
              initial={{ opacity: 0, y: 28, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 18, scale: 0.94 }}
              transition={{ type: 'spring', stiffness: 420, damping: 28 }}
              className={`fixed inset-x-4 bottom-24 z-50 mx-auto max-w-md rounded-xl border px-4 py-3 text-sm font-semibold shadow-2xl backdrop-blur lg:bottom-6 ${
                toast.tone === 'success'
                  ? 'border-emerald-300/15 bg-[#10231b]/95 text-emerald-100'
                  : toast.tone === 'info'
                    ? 'border-white/10 bg-surface-raised/95 text-paper'
                    : 'border-red-300/15 bg-[#241114]/95 text-red-100'
              }`}
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
