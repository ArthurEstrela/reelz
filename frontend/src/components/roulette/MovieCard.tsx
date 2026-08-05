import { useState } from 'react'
import { motion } from 'framer-motion'
import type { RouletteMovie } from '../../types/roulette'

const TMDB_IMAGE_BASE_URL = 'https://image.tmdb.org/t/p/w500'
const TMDB_LOGO_BASE_URL = 'https://image.tmdb.org/t/p/original'

interface MovieCardProps {
  movie: RouletteMovie
  onWatchedAndSpinAgain: () => void
  onSaveToWatchlist: () => Promise<boolean>
  onSpinAgain: () => void
  onWatchProvider: () => void
  spinning?: boolean
}

export function MovieCard({
  movie,
  onWatchedAndSpinAgain,
  onSaveToWatchlist,
  onSpinAgain,
  onWatchProvider,
  spinning = false,
}: MovieCardProps) {
  const [imageFailed, setImageFailed] = useState(false)
  const [markingWatched, setMarkingWatched] = useState(false)
  const [savingToWatchlist, setSavingToWatchlist] = useState(false)
  const [savedToWatchlist, setSavedToWatchlist] = useState(false)
  const offer = movie.streamingAvailability[0]
  const posterUrl = movie.posterPath && !imageFailed ? `${TMDB_IMAGE_BASE_URL}${movie.posterPath}` : null
  const releaseYear = movie.releaseDate?.slice(0, 4)
  const formattedRating = movie.tmdbRating === null ? null : Number(movie.tmdbRating).toFixed(1)

  function handleWatchedClick() {
    if (markingWatched || spinning) return
    setMarkingWatched(true)
    onWatchedAndSpinAgain()
  }

  async function handleWatchlistClick() {
    if (savingToWatchlist || savedToWatchlist || spinning) return
    setSavingToWatchlist(true)
    const saved = await onSaveToWatchlist()
    setSavingToWatchlist(false)
    if (saved) setSavedToWatchlist(true)
  }

  return (
    <motion.article
      key={movie.id}
      initial={{ opacity: 0, scale: 0.5, y: 55, rotate: -3 }}
      animate={{ opacity: 1, scale: 1, y: 0, rotate: 0 }}
      exit={{ opacity: 0, scale: 0.82, y: -30 }}
      transition={{ type: 'spring', bounce: 0.48, duration: 0.86 }}
      className="mx-auto w-full max-w-xl overflow-hidden rounded-2xl border border-white/12 bg-surface shadow-[0_30px_90px_rgba(0,0,0,.48)]"
    >
      <div className="grid grid-cols-[8.25rem_1fr] gap-4 p-3 sm:grid-cols-[11.5rem_1fr] sm:gap-6 sm:p-5">
        <div className="aspect-[2/3] overflow-hidden rounded-xl bg-white/[0.05] shadow-xl">
          {posterUrl ? (
            <img
              src={posterUrl}
              alt={`Pôster de ${movie.title}`}
              onError={() => setImageFailed(true)}
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="grid h-full place-items-center px-4 text-center text-xs font-semibold text-white/55">
              Pôster indisponível
            </div>
          )}
        </div>

        <div className="flex min-w-0 flex-col text-left">
          <div className="flex flex-wrap items-center gap-2 text-xs font-semibold">
            {formattedRating ? (
              <span className="rounded-lg bg-gold/12 px-2.5 py-1 text-gold">★ {formattedRating}</span>
            ) : null}
            {releaseYear ? <span className="text-white/55">{releaseYear}</span> : null}
          </div>
          <h2 className="mt-3 text-2xl font-extrabold leading-tight tracking-[-0.035em] text-paper sm:text-3xl">{movie.title}</h2>
          <p className="mt-3 line-clamp-6 text-xs leading-5 text-white/60 sm:text-sm sm:leading-6">
            {movie.overview || 'A sinopse ainda não chegou à cabine de projeção.'}
          </p>
        </div>
      </div>

      <div className="border-t border-white/8 p-3 sm:p-5">
        {offer?.attributionUrl ? (
          <motion.a
            href={offer.attributionUrl}
            target="_blank"
            rel="noreferrer"
            onClick={onWatchProvider}
            whileTap={{ scale: 0.97 }}
            aria-label="Ver onde assistir"
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-reel px-5 py-3.5 text-sm font-bold text-white shadow-[0_12px_30px_rgba(233,54,69,.22)] transition hover:bg-reel-bright focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-reel"
          >
            {offer.logoPath ? (
              <img src={`${TMDB_LOGO_BASE_URL}${offer.logoPath}`} alt="" className="size-6 rounded-md object-cover" />
            ) : null}
            Assistir na {offer.providerName}
          </motion.a>
        ) : (
          <button
            type="button"
            disabled
            title="O provedor não informou um link de reprodução."
            className="w-full cursor-not-allowed rounded-xl bg-white/[0.07] px-5 py-3.5 text-sm font-semibold text-white/50"
          >
            {offer ? `Disponível na ${offer.providerName}` : 'Streaming indisponível'}
          </button>
        )}

        {offer ? (
          <p className="mt-2 text-center text-[0.68rem] font-medium text-white/50">
            Disponibilidade fornecida pelo JustWatch via TMDB.
          </p>
        ) : null}

        <div className="mt-3 grid grid-cols-2 gap-2">
          <motion.button
            type="button"
            onClick={() => void handleWatchlistClick()}
            disabled={spinning || savingToWatchlist || savedToWatchlist}
            whileTap={{ scale: 0.96 }}
            className="flex min-h-12 items-center justify-center gap-2 rounded-xl border border-white/12 px-3 text-sm font-semibold text-white/75 transition hover:border-white/25 hover:bg-white/[0.05] hover:text-white disabled:cursor-default disabled:opacity-55 focus-visible:outline-2 focus-visible:outline-offset-2"
          >
            <span aria-hidden="true">{savedToWatchlist ? '✓' : '+'}</span>
            {savedToWatchlist
              ? 'Salvo em Quero Ver'
              : savingToWatchlist
                ? 'Salvando…'
                : 'Quero ver depois'}
          </motion.button>

          <motion.button
            type="button"
            onClick={onSpinAgain}
            disabled={spinning || markingWatched || savingToWatchlist}
            whileTap={{ scale: 0.96 }}
            className="min-h-12 rounded-xl bg-paper px-3 text-sm font-bold text-canvas transition hover:bg-white disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
          >
            Girar novamente
          </motion.button>
        </div>

        <motion.button
          type="button"
          onClick={handleWatchedClick}
          disabled={spinning || markingWatched}
          whileTap={{ scale: 0.96 }}
          className="mt-3 w-full px-4 py-2.5 text-sm font-medium text-white/60 underline decoration-white/20 underline-offset-4 transition hover:text-white disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-reel"
        >
          {markingWatched ? 'Marcando como visto…' : 'Já assisti · marcar e girar'}
        </motion.button>
      </div>
    </motion.article>
  )
}
