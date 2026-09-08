import { useEffect, useId, useState } from 'react'
import { motion } from 'framer-motion'
import type { LibraryMovie } from '../../types/history'
import { resolveCatalogImageUrl } from '../../utils/catalogImage'

interface LibraryMovieDetailsProps {
  movie: LibraryMovie
  onClose: () => void
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long' }).format(new Date(value))
}

export function LibraryMovieDetails({ movie, onClose }: LibraryMovieDetailsProps) {
  const titleId = useId()
  const descriptionId = useId()
  const [imageFailed, setImageFailed] = useState(false)
  const posterUrl = imageFailed ? null : resolveCatalogImageUrl(movie.posterPath, 'w500')
  const releaseYear = movie.releaseDate?.slice(0, 4)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }

    window.addEventListener('keydown', closeOnEscape)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [onClose])

  return (
    <div className="fixed inset-0 z-[80] flex items-end justify-center sm:items-center sm:p-6">
      <motion.button
        type="button"
        aria-label="Fechar detalhes do filme"
        className="absolute inset-0 h-full w-full bg-black/75 backdrop-blur-sm"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
      />

      <motion.section
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        initial={{ opacity: 0, y: 80, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 55, scale: 0.97 }}
        transition={{ type: 'spring', stiffness: 360, damping: 32 }}
        className="relative max-h-[88svh] w-full overflow-y-auto rounded-t-[1.75rem] border border-white/10 bg-surface-raised p-5 pb-8 shadow-2xl sm:max-w-2xl sm:rounded-3xl sm:p-7"
      >
        <div className="mx-auto mb-5 h-1 w-10 rounded-full bg-white/20 sm:hidden" aria-hidden="true" />

        <button
          type="button"
          onClick={onClose}
          autoFocus
          aria-label="Fechar"
          className="absolute right-4 top-4 z-10 grid size-10 place-items-center rounded-full border border-white/10 bg-black/45 text-xl text-white/70 backdrop-blur transition hover:bg-white/10 hover:text-white sm:right-5 sm:top-5"
        >
          <span aria-hidden="true">×</span>
        </button>

        <div className="grid grid-cols-[96px_1fr] gap-4 pr-8 sm:grid-cols-[190px_1fr] sm:gap-7 sm:pr-4">
          <div className="aspect-[2/3] overflow-hidden rounded-xl border border-white/10 bg-surface shadow-xl sm:row-span-2 sm:rounded-2xl">
            {posterUrl ? (
              <img
                src={posterUrl}
                alt={`Pôster de ${movie.title}`}
                onError={() => setImageFailed(true)}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="grid h-full place-items-center px-3 text-center text-xs font-semibold text-white/55">
                {movie.title}
              </div>
            )}
          </div>

          <div className="min-w-0 self-center sm:self-start">
            <p className="cinegiro-kicker">
              {movie.status === 'WATCHED' ? 'Na sua coleção' : 'Na sua lista'}
            </p>
            <h2 id={titleId} className="mt-2 text-2xl font-extrabold leading-tight tracking-[-0.035em] text-paper sm:text-4xl">
              {movie.title}
            </h2>

            <div className="mt-4 flex flex-wrap gap-2 text-xs font-semibold text-white/65">
              {releaseYear ? <span className="rounded-full bg-white/[0.06] px-3 py-1.5">{releaseYear}</span> : null}
              {movie.tmdbRating !== null ? (
                <span className="rounded-full bg-gold/10 px-3 py-1.5 text-gold">
                  ★ {movie.tmdbRating.toFixed(1)}
                </span>
              ) : null}
              {movie.rating !== null ? (
                <span className="rounded-full bg-white/[0.06] px-3 py-1.5">Sua nota: {movie.rating}/5</span>
              ) : null}
            </div>

            {movie.watchedAt ? (
              <p className="mt-4 text-xs leading-5 text-white/45">
                Assistido em {formatDate(movie.watchedAt)}
              </p>
            ) : null}
          </div>

          <div className="col-span-2 mt-2 sm:col-span-1 sm:col-start-2 sm:mt-0">
            <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-white/40">Sinopse</p>
            <p id={descriptionId} className="mt-2 text-sm leading-6 text-white/72">
              {movie.overview?.trim() || 'A sinopse deste título ainda não está disponível.'}
            </p>
          </div>
        </div>
      </motion.section>
    </div>
  )
}
