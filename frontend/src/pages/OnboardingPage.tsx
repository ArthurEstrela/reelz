import { AnimatePresence, motion } from 'framer-motion'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { ReelzLogo } from '../components/brand/ReelzLogo'
import { SwipeMovieCard } from '../components/onboarding/SwipeMovieCard'
import { useAuth } from '../hooks/useAuth'
import {
  completeOnboarding,
  getOnboardingMovies,
} from '../services/onboardingService'
import type { OnboardingMovie } from '../types/onboarding'
import { getApiErrorMessage } from '../utils/apiError'

type LoadingState = 'loading' | 'ready' | 'error'

export function OnboardingPage() {
  const navigate = useNavigate()
  const { markOnboardingCompleted } = useAuth()
  const [movies, setMovies] = useState<OnboardingMovie[]>([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [watchedMovieIds, setWatchedMovieIds] = useState<number[]>([])
  const [exitDirection, setExitDirection] = useState(1)
  const [loadingState, setLoadingState] = useState<LoadingState>('loading')
  const [reloadKey, setReloadKey] = useState(0)
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    getOnboardingMovies()
      .then((response) => {
        if (cancelled) return
        setMovies(response.movies)
        setCurrentIndex(0)
        setWatchedMovieIds([])
        setLoadingState(response.movies.length > 0 ? 'ready' : 'error')
        if (response.movies.length === 0) {
          setErrorMessage('O catálogo ainda está aquecendo. Tente novamente em instantes.')
        }
      })
      .catch((error) => {
        if (cancelled) return
        setLoadingState('error')
        setErrorMessage(
          getApiErrorMessage(error, 'Não foi possível preparar seus filmes. Tente novamente.'),
        )
      })

    return () => {
      cancelled = true
    }
  }, [reloadKey])

  function retryLoading() {
    setLoadingState('loading')
    setErrorMessage(null)
    setReloadKey((key) => key + 1)
  }

  const currentMovie = movies[currentIndex]
  const finished = movies.length > 0 && currentIndex >= movies.length
  const progress = movies.length === 0
    ? 0
    : Math.round((Math.min(currentIndex, movies.length) / movies.length) * 100)

  const decide = useCallback((watched: boolean) => {
    const movie = movies[currentIndex]
    if (!movie || submitting) return

    setExitDirection(watched ? 1 : -1)
    if (watched) {
      setWatchedMovieIds((current) => [...current, movie.movieId])
    }
    setCurrentIndex((index) => index + 1)
  }, [currentIndex, movies, submitting])

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'ArrowLeft') decide(false)
      if (event.key === 'ArrowRight') decide(true)
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [decide])

  const previewMovies = useMemo(
    () => movies.slice(currentIndex + 1, currentIndex + 3),
    [currentIndex, movies],
  )

  async function finishOnboarding() {
    if (submitting) return
    setSubmitting(true)
    setErrorMessage(null)

    try {
      await completeOnboarding({
        presentedMovieIds: movies.map((movie) => movie.movieId),
        watchedMovieIds,
      })
      markOnboardingCompleted()
      navigate('/', { replace: true })
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(error, 'Não foi possível salvar suas escolhas. Tente novamente.'),
      )
      setSubmitting(false)
    }
  }

  return (
    <main className="relative min-h-svh overflow-hidden bg-canvas px-5 pt-[max(1.25rem,env(safe-area-inset-top))] pb-8 text-white">
      <div className="pointer-events-none absolute -top-32 left-1/2 h-80 w-80 -translate-x-1/2 rounded-full bg-reel/15 blur-[100px]" />

      <div className="relative mx-auto flex min-h-[calc(100svh-3rem)] w-full max-w-md flex-col">
        <header className="flex items-center justify-between">
          <ReelzLogo />
          {movies.length > 0 && (
            <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-extrabold text-white/60">
              {Math.min(currentIndex + 1, movies.length)} de {movies.length}
            </span>
          )}
        </header>

        <div className="mt-6">
          <p className="text-xs font-black tracking-[.18em] text-reel-bright uppercase">
            Ajuste seu Reelz
          </p>
          <h1 className="mt-2 text-3xl leading-tight font-black tracking-tight">
            O que você já assistiu?
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-white/50">
            Deslize para a direita se já viu. Assim a roleta não repete o óbvio.
          </p>
          <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-white/8">
            <motion.div
              className="h-full rounded-full bg-gradient-to-r from-reel to-amber-300"
              animate={{ width: `${progress}%` }}
              transition={{ type: 'spring', stiffness: 180, damping: 24 }}
            />
          </div>
        </div>

        <section className="relative mt-6 flex min-h-0 flex-1 flex-col">
          {loadingState === 'loading' && (
            <div className="relative mx-auto aspect-[2/3] w-full max-w-[19rem] animate-pulse overflow-hidden rounded-[2rem] border border-white/8 bg-white/5">
              <div className="absolute inset-x-6 bottom-7 space-y-3">
                <div className="h-6 w-3/4 rounded-full bg-white/8" />
                <div className="h-4 w-1/3 rounded-full bg-white/8" />
              </div>
            </div>
          )}

          {loadingState === 'error' && (
            <div className="my-auto rounded-[2rem] border border-white/10 bg-white/[0.04] p-8 text-center">
              <span className="text-4xl" aria-hidden="true">🎬</span>
              <p role="alert" className="mt-4 text-sm leading-relaxed text-white/65">
                {errorMessage}
              </p>
              <motion.button
                type="button"
                whileTap={{ scale: 0.96 }}
                onClick={retryLoading}
                className="mt-6 rounded-2xl bg-white px-6 py-3 text-sm font-black text-black"
              >
                Tentar novamente
              </motion.button>
            </div>
          )}

          {loadingState === 'ready' && !finished && (
            <>
              <div className="relative mx-auto aspect-[2/3] w-full max-w-[19rem]">
                {previewMovies.slice().reverse().map((movie, reverseIndex) => {
                  const depth = previewMovies.length - reverseIndex
                  return (
                    <div
                      key={movie.movieId}
                      aria-hidden="true"
                      className="absolute inset-0 overflow-hidden rounded-[2rem] border border-white/8 bg-[#17171e]"
                      style={{
                        transform: `translateY(${depth * 9}px) scale(${1 - depth * 0.035})`,
                        opacity: 1 - depth * 0.22,
                      }}
                    >
                      {movie.posterPath && (
                        <img
                          src={`https://image.tmdb.org/t/p/w500${movie.posterPath}`}
                          alt=""
                          className="h-full w-full object-cover"
                        />
                      )}
                    </div>
                  )
                })}

                <AnimatePresence initial={false} custom={exitDirection}>
                  {currentMovie && (
                    <SwipeMovieCard
                      key={currentMovie.movieId}
                      movie={currentMovie}
                      exitDirection={exitDirection}
                      onDecision={decide}
                    />
                  )}
                </AnimatePresence>
              </div>

              <div className="mt-7 grid grid-cols-2 gap-3">
                <motion.button
                  type="button"
                  whileTap={{ scale: 0.94 }}
                  onClick={() => decide(false)}
                  className="rounded-2xl border border-white/10 bg-white/5 px-4 py-4 text-sm font-black text-white/70"
                >
                  ← Não assisti
                </motion.button>
                <motion.button
                  type="button"
                  whileTap={{ scale: 0.94 }}
                  onClick={() => decide(true)}
                  className="rounded-2xl bg-emerald-400 px-4 py-4 text-sm font-black text-emerald-950 shadow-[0_12px_35px_rgba(52,211,153,.2)]"
                >
                  Já assisti →
                </motion.button>
              </div>
            </>
          )}

          {finished && (
            <motion.div
              initial={{ scale: 0.7, opacity: 0, y: 25 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              transition={{ type: 'spring', stiffness: 230, damping: 18, bounce: 0.42 }}
              className="my-auto rounded-[2rem] border border-white/10 bg-gradient-to-br from-white/[0.07] to-reel/10 p-8 text-center shadow-2xl"
            >
              <div className="mx-auto flex size-20 items-center justify-center rounded-full bg-emerald-400/15 text-4xl">
                ✓
              </div>
              <h2 className="mt-6 text-2xl font-black">Seu perfil ganhou forma.</h2>
              <p className="mt-3 text-sm leading-relaxed text-white/55">
                Marcamos {watchedMovieIds.length}{' '}
                {watchedMovieIds.length === 1
                  ? 'filme como assistido'
                  : 'filmes como assistidos'}.
                Agora sua roleta começa muito mais esperta.
              </p>
              {errorMessage && (
                <p role="alert" className="mt-4 text-sm font-semibold text-red-300">
                  {errorMessage}
                </p>
              )}
              <motion.button
                type="button"
                whileTap={{ scale: 0.96 }}
                disabled={submitting}
                onClick={() => void finishOnboarding()}
                className="mt-7 w-full rounded-2xl bg-reel px-5 py-4 text-sm font-black text-white shadow-[0_14px_40px_rgba(255,60,72,.25)] disabled:cursor-wait disabled:opacity-60"
              >
                {submitting ? 'Salvando escolhas...' : 'Começar a girar'}
              </motion.button>
            </motion.div>
          )}
        </section>
      </div>
    </main>
  )
}
