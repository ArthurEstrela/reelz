import {
  motion,
  useMotionValue,
  useTransform,
  type PanInfo,
} from 'framer-motion'
import type { OnboardingMovie } from '../../types/onboarding'
import { resolveCatalogImageUrl } from '../../utils/catalogImage'
const SWIPE_DISTANCE = 105
const SWIPE_VELOCITY = 650

interface SwipeMovieCardProps {
  movie: OnboardingMovie
  exitDirection: number
  onDecision: (watched: boolean) => void
}

export function SwipeMovieCard({
  movie,
  exitDirection,
  onDecision,
}: SwipeMovieCardProps) {
  const x = useMotionValue(0)
  const rotate = useTransform(x, [-220, 0, 220], [-11, 0, 11])
  const watchedOpacity = useTransform(x, [20, 120], [0, 1])
  const unseenOpacity = useTransform(x, [-120, -20], [1, 0])
  const posterUrl = resolveCatalogImageUrl(movie.posterPath)

  function handleDragEnd(_event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) {
    const swipedRight =
      info.offset.x > SWIPE_DISTANCE || info.velocity.x > SWIPE_VELOCITY
    const swipedLeft =
      info.offset.x < -SWIPE_DISTANCE || info.velocity.x < -SWIPE_VELOCITY

    if (swipedRight) onDecision(true)
    if (swipedLeft) onDecision(false)
  }

  return (
    <motion.article
      className="absolute inset-0 touch-pan-y cursor-grab overflow-hidden rounded-[2rem] border border-white/10 bg-[#17171e] shadow-[0_28px_90px_rgba(0,0,0,.55)] active:cursor-grabbing"
      style={{ x, rotate }}
      initial={{ scale: 0.92, y: 26, opacity: 0 }}
      animate={{ scale: 1, y: 0, opacity: 1 }}
      exit={{
        x: exitDirection * 520,
        rotate: exitDirection * 18,
        opacity: 0,
        transition: { duration: 0.26, ease: 'easeOut' },
      }}
      transition={{ type: 'spring', stiffness: 310, damping: 25, bounce: 0.28 }}
      drag="x"
      dragConstraints={{ left: 0, right: 0 }}
      dragElastic={0.92}
      dragSnapToOrigin
      onDragEnd={handleDragEnd}
      aria-label={`${movie.title}. Arraste para a direita se já assistiu ou para a esquerda se ainda não viu.`}
    >
      {posterUrl ? (
        <img
          src={posterUrl}
          alt={`Pôster de ${movie.title}`}
          className="h-full w-full object-cover"
          draggable={false}
        />
      ) : (
        <div className="flex h-full items-center justify-center bg-gradient-to-br from-white/8 to-brand/15 px-8 text-center text-xl font-black text-white/70">
          {movie.title}
        </div>
      )}

      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-2/5 bg-gradient-to-t from-black via-black/65 to-transparent" />

      <motion.div
        style={{ opacity: unseenOpacity }}
        className="pointer-events-none absolute top-7 left-6 -rotate-8 rounded-xl border-4 border-amber-300 px-4 py-2 text-xl font-black tracking-[.12em] text-amber-300 uppercase"
      >
        Não vi
      </motion.div>
      <motion.div
        style={{ opacity: watchedOpacity }}
        className="pointer-events-none absolute top-7 right-6 rotate-8 rounded-xl border-4 border-emerald-300 px-4 py-2 text-xl font-black tracking-[.12em] text-emerald-300 uppercase"
      >
        Já vi
      </motion.div>

      <div className="pointer-events-none absolute inset-x-0 bottom-0 p-6">
        <h2 className="line-clamp-2 text-2xl leading-tight font-black text-white">
          {movie.title}
        </h2>
        {movie.voteAverage !== null && (
          <p className="mt-2 flex items-center gap-1.5 text-sm font-bold text-amber-300">
            <span aria-hidden="true">★</span>
            {movie.voteAverage.toFixed(1)} no catálogo
          </p>
        )}
      </div>
    </motion.article>
  )
}
