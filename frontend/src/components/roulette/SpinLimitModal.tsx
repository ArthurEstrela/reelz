import { useEffect } from 'react'
import { motion } from 'framer-motion'

interface SpinLimitModalProps {
  onClose: () => void
}

export function SpinLimitModal({ onClose }: SpinLimitModalProps) {
  useEffect(() => {
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }

    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose])

  return (
    <motion.div
      className="fixed inset-0 z-50 grid place-items-center bg-black/75 px-5 backdrop-blur-sm"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onMouseDown={(event) => {
        if (event.currentTarget === event.target) onClose()
      }}
    >
      <motion.div
        role="dialog"
        aria-modal="true"
        aria-labelledby="spin-limit-title"
        initial={{ opacity: 0, scale: 0.65, y: 50 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.85, y: 24 }}
        transition={{ type: 'spring', bounce: 0.46, duration: 0.72 }}
        className="w-full max-w-sm rounded-[2rem] border border-white/10 bg-surface p-6 text-center shadow-[0_30px_100px_rgba(0,0,0,.65)]"
      >
        <div className="mx-auto grid size-16 place-items-center rounded-2xl bg-reel/15 text-3xl" aria-hidden="true">
          🎟️
        </div>
        <h2 id="spin-limit-title" className="mt-5 text-2xl font-black tracking-tight">
          Seus giros acabaram
        </h2>
        <p className="mt-3 text-sm leading-6 text-white/55">
          Volte amanhã ou ganhe mais giros com um anúncio quando a recompensa estiver disponível.
        </p>
        <button
          type="button"
          onClick={onClose}
          autoFocus
          className="mt-6 w-full rounded-2xl bg-reel px-5 py-3.5 text-sm font-black text-white transition hover:bg-reel-bright focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-reel"
        >
          Entendi
        </button>
      </motion.div>
    </motion.div>
  )
}
