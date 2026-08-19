import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { AnimatePresence, motion } from 'framer-motion'
import { AppHeader } from '../components/navigation/AppHeader'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import {
  getWatchedHistory,
  getWatchlist,
  markMovieAsWatched,
  removeMovieFromWatchlist,
} from '../services/historyService'
import type { LibraryMovie, UserMovieStatus } from '../types/history'
import type { PageResponse } from '../types/pagination'
import { getApiErrorMessage } from '../utils/apiError'
import { useAchievements } from '../hooks/useAchievements'

const PAGE_SIZE = 24
const TMDB_IMAGE_BASE_URL = 'https://image.tmdb.org/t/p/w500'

const tabs: Array<{ status: UserMovieStatus; label: string }> = [
  { status: 'WATCHED', label: 'Assistidos' },
  { status: 'WATCHLIST', label: 'Quero Ver' },
]

interface LibraryPosterProps {
  movie: LibraryMovie
  index: number
  pending: boolean
  onMarkWatched: (movie: LibraryMovie) => void
  onRemove: (movie: LibraryMovie) => void
}

function LibraryPoster({
  movie,
  index,
  pending,
  onMarkWatched,
  onRemove,
}: LibraryPosterProps) {
  const [imageFailed, setImageFailed] = useState(false)
  const posterUrl = movie.posterPath && !imageFailed
    ? `${TMDB_IMAGE_BASE_URL}${movie.posterPath}`
    : null
  const watchedDate = movie.watchedAt
    ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' }).format(new Date(movie.watchedAt))
    : null
  const isWatchlist = movie.status === 'WATCHLIST'

  return (
    <motion.article
      layout
      initial={{ opacity: 0, scale: 0.82, y: 22 }}
      animate={{ opacity: pending ? 0.45 : 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.72, y: -18 }}
      transition={{ type: 'spring', bounce: 0.32, delay: Math.min(index, 10) * 0.035 }}
      className="group relative aspect-[2/3] overflow-hidden rounded-lg border border-white/10 bg-surface shadow-xl"
      title={watchedDate ? `${movie.title} · assistido em ${watchedDate}` : movie.title}
    >
      {posterUrl ? (
        <img
          src={posterUrl}
          alt={`Pôster de ${movie.title}`}
          loading="lazy"
          onError={() => setImageFailed(true)}
          className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
        />
      ) : (
        <div className="grid h-full place-items-center px-3 text-center text-xs font-semibold text-white/55">
          {movie.title}
        </div>
      )}

      {isWatchlist ? (
        <>
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black via-black/15 to-transparent" />
          <div className="absolute inset-x-2 bottom-2 grid grid-cols-[1fr_auto] gap-1.5">
            <button
              type="button"
              disabled={pending}
              onClick={() => onMarkWatched(movie)}
              aria-label={`Marcar ${movie.title} como assistido`}
              className="truncate rounded-lg bg-emerald-400 px-2 py-3 text-[10px] font-bold text-emerald-950 shadow-lg transition hover:bg-emerald-300 disabled:cursor-wait"
            >
              ✓ Já assisti
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => onRemove(movie)}
              aria-label={`Remover ${movie.title} da lista Quero Ver`}
              className="grid size-10 place-items-center rounded-xl border border-white/15 bg-black/65 text-base text-white/75 backdrop-blur transition hover:border-red-300/35 hover:text-red-200 disabled:cursor-wait"
            >
              <span aria-hidden="true">×</span>
            </button>
          </div>
        </>
      ) : (
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-1/3 bg-gradient-to-t from-black/65 to-transparent opacity-0 transition group-hover:opacity-100" />
      )}
    </motion.article>
  )
}

function PosterSkeletons() {
  return (
    <div
      className="grid grid-cols-3 gap-3 sm:grid-cols-4 sm:gap-4 md:grid-cols-5 lg:grid-cols-6"
      role="status"
      aria-label="Carregando biblioteca"
    >
      <span className="sr-only">Carregando sua coleção…</span>
      {Array.from({ length: 12 }, (_, index) => (
        <motion.div
          key={index}
          animate={{ opacity: [0.2, 0.5, 0.2] }}
          transition={{ duration: 1.2, delay: (index % 6) * 0.07, repeat: Infinity }}
          className="aspect-[2/3] rounded-2xl bg-white/10"
          aria-hidden="true"
        />
      ))}
    </div>
  )
}

async function fetchHistory(
  status: UserMovieStatus,
  page: number,
): Promise<PageResponse<LibraryMovie>> {
  return status === 'WATCHED'
    ? getWatchedHistory(page, PAGE_SIZE)
    : getWatchlist(page, PAGE_SIZE)
}

export function LibraryPage() {
  const { refreshAchievements } = useAchievements()
  const requestSequence = useRef(0)
  const requestInFlight = useRef(false)
  const [activeStatus, setActiveStatus] = useState<UserMovieStatus>('WATCHED')
  const [movies, setMovies] = useState<LibraryMovie[]>([])
  const [nextPage, setNextPage] = useState(0)
  const [totals, setTotals] = useState<Record<UserMovieStatus, number | null>>({
    WATCHED: null,
    WATCHLIST: null,
  })
  const [lastPage, setLastPage] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [pendingMovieIds, setPendingMovieIds] = useState<Set<number>>(new Set())
  const [error, setError] = useState<string | null>(null)
  const [mutationError, setMutationError] = useState<string | null>(null)

  const loadPage = useCallback(async (
    status: UserMovieStatus,
    page: number,
    replace: boolean,
  ) => {
    if (requestInFlight.current) return
    requestInFlight.current = true
    const sequence = ++requestSequence.current
    if (replace) setInitialLoading(true)
    else setLoadingMore(true)
    setError(null)

    try {
      const response = await fetchHistory(status, page)
      if (sequence !== requestSequence.current) return
      setMovies((current) => {
        if (replace) return response.content
        const knownIds = new Set(current.map((movie) => movie.id))
        return [...current, ...response.content.filter((movie) => !knownIds.has(movie.id))]
      })
      setNextPage(response.page.number + 1)
      setTotals((current) => ({
        ...current,
        [status]: response.page.totalElements,
      }))
      setLastPage(response.page.number + 1 >= response.page.totalPages)
    } catch (loadError) {
      if (sequence !== requestSequence.current) return
      setError(getApiErrorMessage(loadError, 'Não foi possível abrir sua biblioteca agora.'))
    } finally {
      if (sequence === requestSequence.current) {
        requestInFlight.current = false
        setInitialLoading(false)
        setLoadingMore(false)
      }
    }
  }, [])

  useEffect(() => {
    const sequenceRef = requestSequence
    const inFlightRef = requestInFlight
    const timeout = window.setTimeout(
      () => void loadPage(activeStatus, 0, true),
      0,
    )
    return () => {
      window.clearTimeout(timeout)
      sequenceRef.current++
      inFlightRef.current = false
    }
  }, [activeStatus, loadPage])

  function selectTab(status: UserMovieStatus) {
    if (status === activeStatus) return
    requestSequence.current++
    requestInFlight.current = false
    setMovies([])
    setNextPage(0)
    setLastPage(false)
    setError(null)
    setMutationError(null)
    setInitialLoading(true)
    setActiveStatus(status)
  }

  async function mutateWatchlist(
    movie: LibraryMovie,
    action: 'WATCHED' | 'REMOVE',
  ) {
    if (pendingMovieIds.has(movie.movieId)) return
    const originalIndex = movies.findIndex((item) => item.id === movie.id)
    setMutationError(null)
    setPendingMovieIds((current) => new Set(current).add(movie.movieId))
    setMovies((current) => current.filter((item) => item.id !== movie.id))
    setTotals((current) => ({
      ...current,
      WATCHLIST: Math.max(0, (current.WATCHLIST ?? 1) - 1),
    }))

    try {
      if (action === 'WATCHED') {
        await markMovieAsWatched(movie.movieId)
        void refreshAchievements()
      } else {
        await removeMovieFromWatchlist(movie.movieId)
      }
    } catch (mutationFailure) {
      setMovies((current) => {
        const restored = [...current]
        restored.splice(Math.max(0, originalIndex), 0, movie)
        return restored
      })
      setTotals((current) => ({
        ...current,
        WATCHLIST: (current.WATCHLIST ?? 0) + 1,
      }))
      setMutationError(
        getApiErrorMessage(
          mutationFailure,
          action === 'WATCHED'
            ? `Não foi possível marcar “${movie.title}” como assistido.`
            : `Não foi possível remover “${movie.title}” da lista.`,
        ),
      )
    } finally {
      setPendingMovieIds((current) => {
        const next = new Set(current)
        next.delete(movie.movieId)
        return next
      })
    }
  }

  const totalElements = totals[activeStatus]
  const isWatchedTab = activeStatus === 'WATCHED'

  return (
    <main className="relative min-h-svh overflow-hidden bg-canvas px-4 py-5 pb-28 text-paper sm:px-8 sm:py-6 lg:pb-12">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-96 bg-[radial-gradient(circle_at_18%_0%,rgba(233,54,69,.11),transparent_34%)]" />

      <AppHeader />

      <section className="relative mx-auto max-w-6xl pt-10 sm:pt-14">
        <p className="reelz-kicker">Sua videoteca</p>
        <div className="mt-2 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-4xl font-extrabold tracking-[-0.045em] sm:text-6xl">Biblioteca</h1>
            <p className="mt-3 text-sm text-white/60 sm:text-base">
              {initialLoading || totalElements === null
                ? 'Organizando seus filmes…'
                : isWatchedTab
                  ? `Você já colecionou ${totalElements} ${totalElements === 1 ? 'filme' : 'filmes'}.`
                  : `${totalElements} ${totalElements === 1 ? 'filme esperando' : 'filmes esperando'} por você.`}
            </p>
          </div>
          {!initialLoading && totalElements !== null && totalElements > 0 ? (
            <div className="rounded-xl border border-white/10 bg-white/[0.04] px-4 py-3 text-center">
              <strong className="block text-2xl font-bold text-reel-bright">{totalElements}</strong>
              <span className="text-[10px] font-semibold uppercase tracking-wider text-white/55">
                {isWatchedTab ? 'na coleção' : 'para assistir'}
              </span>
            </div>
          ) : null}
        </div>

        <div
          className="mt-7 grid max-w-sm grid-cols-2 rounded-xl border border-white/10 bg-white/[0.035] p-1"
          role="tablist"
          aria-label="Seções da biblioteca"
        >
          {tabs.map((tab) => {
            const active = tab.status === activeStatus
            const knownTotal = totals[tab.status]
            return (
              <button
                key={tab.status}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => selectTab(tab.status)}
                className={`relative rounded-lg px-3 py-2.5 text-xs font-semibold transition ${
                  active ? 'text-paper' : 'text-white/55 hover:text-white/80'
                }`}
              >
                {active && (
                  <motion.span
                    layoutId="library-active-tab"
                    className="absolute inset-0 rounded-lg bg-white/10"
                    transition={{ type: 'spring', stiffness: 420, damping: 32 }}
                  />
                )}
                <span className="relative">
                  {tab.label}
                  {knownTotal !== null ? ` · ${knownTotal}` : ''}
                </span>
              </button>
            )
          })}
        </div>

        <div className="mt-7">
          {initialLoading ? <PosterSkeletons /> : null}

          {!initialLoading && movies.length > 0 ? (
            <motion.div
              layout
              className={`grid gap-3 sm:gap-4 ${
                isWatchedTab
                  ? 'grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6'
                  : 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5'
              }`}
            >
              <AnimatePresence mode="popLayout">
                {movies.map((movie, index) => (
                  <LibraryPoster
                    key={movie.id}
                    movie={movie}
                    index={index}
                    pending={pendingMovieIds.has(movie.movieId)}
                    onMarkWatched={(selectedMovie) => {
                      void mutateWatchlist(selectedMovie, 'WATCHED')
                    }}
                    onRemove={(selectedMovie) => {
                      void mutateWatchlist(selectedMovie, 'REMOVE')
                    }}
                  />
                ))}
              </AnimatePresence>
            </motion.div>
          ) : null}

          {!initialLoading && movies.length === 0 && !error ? (
            <div className="mx-auto max-w-md border-y border-white/10 px-7 py-12 text-center">
              <svg viewBox="0 0 24 24" className="mx-auto size-11 text-reel-bright" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
                {isWatchedTab ? <><rect x="4" y="5" width="16" height="15" rx="2" /><path d="m4 10 16-3M8 5l3 4M14 4l3 4" /></> : <path d="M7 4h10a1 1 0 0 1 1 1v15l-6-3-6 3V5a1 1 0 0 1 1-1Z" />}
              </svg>
              <h2 className="mt-5 text-xl font-bold">
                {isWatchedTab
                  ? 'Sua coleção começa no próximo giro'
                  : 'Sua lista Quero Ver está livre'}
              </h2>
              <p className="mt-2 text-sm leading-6 text-white/60">
                {isWatchedTab
                  ? 'Marque um resultado como “Já vi” e ele aparecerá aqui.'
                  : 'Salve um resultado da roleta para assistir quando chegar a hora certa.'}
              </p>
              <Link to="/" className="mt-6 inline-flex rounded-xl bg-reel px-5 py-3 text-sm font-bold text-white">
                Girar agora
              </Link>
            </div>
          ) : null}

          {error ? (
            <div className="mx-auto mt-6 max-w-md rounded-2xl border border-red-300/15 bg-red-300/[0.06] p-4 text-center text-sm text-red-100" role="alert">
              <p>{error}</p>
              <button
                type="button"
                onClick={() => void loadPage(
                  activeStatus,
                  movies.length === 0 ? 0 : nextPage,
                  movies.length === 0,
                )}
                className="mt-2 font-bold underline underline-offset-2"
              >
                Tentar novamente
              </button>
            </div>
          ) : null}

          <AnimatePresence>
            {mutationError ? (
              <motion.div
                role="alert"
                initial={{ opacity: 0, y: 20, scale: 0.94 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 12 }}
                className="fixed inset-x-4 bottom-24 z-50 mx-auto max-w-md rounded-2xl border border-red-300/15 bg-[#241114]/95 px-4 py-3 text-sm font-bold text-red-100 shadow-2xl backdrop-blur"
              >
                {mutationError}
              </motion.div>
            ) : null}
          </AnimatePresence>

          {!initialLoading && movies.length > 0 && !lastPage && !error ? (
            <button
              type="button"
              onClick={() => void loadPage(activeStatus, nextPage, false)}
              disabled={loadingMore}
              className="mx-auto mt-8 block rounded-xl border border-white/12 px-6 py-3.5 text-sm font-semibold text-white/70 transition hover:border-white/25 hover:bg-white/[0.04] hover:text-white disabled:opacity-40"
            >
              {loadingMore ? 'Buscando mais filmes…' : 'Carregar mais'}
            </button>
          ) : null}
        </div>
      </section>

      <BottomNavigation />
    </main>
  )
}
