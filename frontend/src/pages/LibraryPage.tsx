import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { motion } from 'framer-motion'
import { ReelzLogo } from '../components/brand/ReelzLogo'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { useAuth } from '../hooks/useAuth'
import { getWatchedHistory } from '../services/historyService'
import type { WatchedMovie } from '../types/history'
import { getApiErrorMessage } from '../utils/apiError'

const PAGE_SIZE = 24
const TMDB_IMAGE_BASE_URL = 'https://image.tmdb.org/t/p/w500'

function LibraryPoster({ movie, index }: { movie: WatchedMovie; index: number }) {
  const [imageFailed, setImageFailed] = useState(false)
  const posterUrl = movie.posterPath && !imageFailed
    ? `${TMDB_IMAGE_BASE_URL}${movie.posterPath}`
    : null
  const watchedDate = movie.watchedAt
    ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' }).format(new Date(movie.watchedAt))
    : 'data não informada'

  return (
    <motion.article
      initial={{ opacity: 0, scale: 0.82, y: 22 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      transition={{ type: 'spring', bounce: 0.32, delay: Math.min(index, 10) * 0.035 }}
      className="group relative aspect-[2/3] overflow-hidden rounded-2xl border border-white/8 bg-surface shadow-xl"
      title={`${movie.title} · assistido em ${watchedDate}`}
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
        <div className="grid h-full place-items-center px-3 text-center text-xs font-bold text-white/35">
          {movie.title}
        </div>
      )}
      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-1/3 bg-gradient-to-t from-black/65 to-transparent opacity-0 transition group-hover:opacity-100" />
    </motion.article>
  )
}

function PosterSkeletons() {
  return (
    <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 sm:gap-4 md:grid-cols-5 lg:grid-cols-6" role="status" aria-label="Carregando biblioteca">
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

export function LibraryPage() {
  const { user, logout } = useAuth()
  const requestSequence = useRef(0)
  const requestInFlight = useRef(false)
  const [movies, setMovies] = useState<WatchedMovie[]>([])
  const [nextPage, setNextPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [lastPage, setLastPage] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadPage = useCallback(async (page: number, replace: boolean) => {
    if (requestInFlight.current) return
    requestInFlight.current = true
    const sequence = ++requestSequence.current
    if (replace) setInitialLoading(true)
    else setLoadingMore(true)
    setError(null)

    try {
      const response = await getWatchedHistory(page, PAGE_SIZE)
      if (sequence !== requestSequence.current) return
      setMovies((current) => {
        if (replace) return response.content
        const knownIds = new Set(current.map((movie) => movie.id))
        return [...current, ...response.content.filter((movie) => !knownIds.has(movie.id))]
      })
      setNextPage(response.page.number + 1)
      setTotalElements(response.page.totalElements)
      setLastPage(response.page.number + 1 >= response.page.totalPages)
    } catch (loadError) {
      if (sequence !== requestSequence.current) return
      setError(getApiErrorMessage(loadError, 'Não foi possível abrir sua coleção agora.'))
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
    const timeout = window.setTimeout(() => void loadPage(0, true), 0)
    return () => {
      window.clearTimeout(timeout)
      sequenceRef.current++
      requestInFlight.current = false
    }
  }, [loadPage])

  return (
    <main className="relative min-h-svh overflow-hidden bg-canvas px-4 py-5 pb-28 text-white sm:px-8 sm:py-6 sm:pb-28">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_0%,rgba(255,60,72,.13),transparent_28%)]" />

      <header className="relative mx-auto flex max-w-6xl items-center justify-between">
        <ReelzLogo />
        <button
          type="button"
          onClick={logout}
          title={user?.email ? `Sair de ${user.email}` : 'Sair'}
          className="rounded-xl border border-white/10 px-3 py-2.5 text-xs font-bold text-white/50 transition hover:border-white/20 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-reel"
        >
          Sair
        </button>
      </header>

      <section className="relative mx-auto max-w-6xl pt-10 sm:pt-14">
        <p className="text-xs font-black uppercase tracking-[0.24em] text-reel">Sua videoteca</p>
        <div className="mt-2 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-4xl font-black tracking-[-0.055em] sm:text-6xl">Biblioteca</h1>
            <p className="mt-3 text-sm text-white/45 sm:text-base">
              {initialLoading
                ? 'Contando seus filmes…'
                : `Você já colecionou ${totalElements} ${totalElements === 1 ? 'filme' : 'filmes'}.`}
            </p>
          </div>
          {!initialLoading && totalElements > 0 ? (
            <div className="rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-3 text-center">
              <strong className="block text-2xl font-black text-reel-bright">{totalElements}</strong>
              <span className="text-[10px] font-bold uppercase tracking-wider text-white/35">na coleção</span>
            </div>
          ) : null}
        </div>

        <div className="mt-9">
          {initialLoading ? <PosterSkeletons /> : null}

          {!initialLoading && movies.length > 0 ? (
            <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 sm:gap-4 md:grid-cols-5 lg:grid-cols-6">
              {movies.map((movie, index) => (
                <LibraryPoster key={movie.id} movie={movie} index={index} />
              ))}
            </div>
          ) : null}

          {!initialLoading && movies.length === 0 && !error ? (
            <div className="mx-auto max-w-md rounded-[2rem] border border-white/8 bg-white/[0.025] px-7 py-10 text-center">
              <span className="text-5xl" aria-hidden="true">🎬</span>
              <h2 className="mt-5 text-xl font-black">Sua coleção começa no próximo giro</h2>
              <p className="mt-2 text-sm leading-6 text-white/45">Marque um resultado como “Já vi” e ele aparecerá aqui.</p>
              <Link to="/" className="mt-6 inline-flex rounded-2xl bg-reel px-5 py-3 text-sm font-black text-white">
                Girar agora
              </Link>
            </div>
          ) : null}

          {error ? (
            <div className="mx-auto mt-6 max-w-md rounded-2xl border border-red-300/15 bg-red-300/[0.06] p-4 text-center text-sm text-red-100" role="alert">
              <p>{error}</p>
              <button type="button" onClick={() => void loadPage(movies.length === 0 ? 0 : nextPage, movies.length === 0)} className="mt-2 font-black underline underline-offset-2">
                Tentar novamente
              </button>
            </div>
          ) : null}

          {!initialLoading && movies.length > 0 && !lastPage && !error ? (
            <button
              type="button"
              onClick={() => void loadPage(nextPage, false)}
              disabled={loadingMore}
              className="mx-auto mt-8 block rounded-2xl border border-white/10 bg-white/[0.04] px-6 py-3.5 text-sm font-black text-white/70 transition hover:border-white/20 hover:text-white disabled:opacity-40"
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
