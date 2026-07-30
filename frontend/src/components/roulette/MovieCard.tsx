import { useState } from 'react'
import { motion } from 'framer-motion'
import type { RouletteMovie } from '../../types/roulette'

const TMDB_IMAGE_BASE_URL = 'https://image.tmdb.org/t/p/w500'
const TMDB_LOGO_BASE_URL = 'https://image.tmdb.org/t/p/original'

interface MovieCardProps {
  movie: RouletteMovie
  onSpinAgain: () => void
  spinning?: boolean
}

export function MovieCard({ movie, onSpinAgain, spinning = false }: MovieCardProps) {
  const [imageFailed, setImageFailed] = useState(false)
  const offer = movie.streamingAvailability[0]
  const posterUrl = movie.posterPath && !imageFailed ? `${TMDB_IMAGE_BASE_URL}${movie.posterPath}` : null
  const releaseYear = movie.releaseDate?.slice(0, 4)
  const formattedRating = movie.tmdbRating === null ? null : Number(movie.tmdbRating).toFixed(1)

  return (
    <motion.article
      key={movie.id}
      initial={{ opacity: 0, scale: 0.5, y: 55, rotate: -3 }}
      animate={{ opacity: 1, scale: 1, y: 0, rotate: 0 }}
      exit={{ opacity: 0, scale: 0.82, y: -30 }}
      transition={{ type: 'spring', bounce: 0.48, duration: 0.86 }}
      className="mx-auto w-full max-w-lg overflow-hidden rounded-[2rem] border border-white/10 bg-surface shadow-[0_30px_100px_rgba(0,0,0,.55)]"
    >
      <div className="grid grid-cols-[8.5rem_1fr] gap-4 p-4 sm:grid-cols-[11rem_1fr] sm:gap-6 sm:p-6">
        <div className="aspect-[2/3] overflow-hidden rounded-2xl bg-white/[0.05] shadow-xl">
          {posterUrl ? (
            <img
              src={posterUrl}
              alt={`Pôster de ${movie.title}`}
              onError={() => setImageFailed(true)}
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="grid h-full place-items-center px-4 text-center text-xs font-bold text-white/30">
              Pôster indisponível
            </div>
          )}
        </div>

        <div className="flex min-w-0 flex-col text-left">
          <div className="flex flex-wrap items-center gap-2 text-xs font-bold">
            {formattedRating ? (
              <span className="rounded-full bg-amber-400/15 px-2.5 py-1 text-amber-300">★ {formattedRating}</span>
            ) : null}
            {releaseYear ? <span className="text-white/35">{releaseYear}</span> : null}
          </div>
          <h2 className="mt-3 text-2xl font-black leading-tight tracking-[-0.04em] sm:text-3xl">{movie.title}</h2>
          <p className="mt-3 line-clamp-6 text-xs leading-5 text-white/50 sm:text-sm sm:leading-6">
            {movie.overview || 'A sinopse ainda não chegou à cabine de projeção.'}
          </p>
        </div>
      </div>

      <div className="border-t border-white/8 p-4 sm:p-6">
        {offer?.attributionUrl ? (
          <motion.a
            href={offer.attributionUrl}
            target="_blank"
            rel="noreferrer"
            whileTap={{ scale: 0.97 }}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-reel px-5 py-4 text-sm font-black text-white shadow-[0_12px_35px_rgba(255,60,72,.28)] transition hover:bg-reel-bright focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-reel"
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
            className="w-full cursor-not-allowed rounded-2xl bg-white/10 px-5 py-4 text-sm font-black text-white/45"
          >
            {offer ? `Assistir na ${offer.providerName}` : 'Streaming indisponível'}
          </button>
        )}

        <motion.button
          type="button"
          onClick={onSpinAgain}
          disabled={spinning}
          whileTap={{ scale: 0.96 }}
          className="mt-3 w-full rounded-2xl border border-white/10 px-5 py-3.5 text-sm font-bold text-white/60 transition hover:border-white/20 hover:bg-white/[0.04] hover:text-white disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-reel"
        >
          Já vi / Girar de novo
        </motion.button>
      </div>
    </motion.article>
  )
}
