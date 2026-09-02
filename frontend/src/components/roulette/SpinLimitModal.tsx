import { useRef } from 'react'
import { motion } from 'framer-motion'
import { Link } from 'react-router'
import { useDialogFocus } from '../../hooks/useDialogFocus'

interface SpinLimitModalProps {
  onClose: () => void
}

export function SpinLimitModal({ onClose }: SpinLimitModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null)
  useDialogFocus(true, dialogRef, onClose)

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
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="spin-limit-title"
        initial={{ opacity: 0, scale: 0.65, y: 50 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.85, y: 24 }}
        transition={{ type: 'spring', bounce: 0.46, duration: 0.72 }}
        className="w-full max-w-sm rounded-2xl border border-white/10 bg-surface-raised p-6 text-center shadow-[0_30px_100px_rgba(0,0,0,.65)]"
      >
        <svg viewBox="0 0 24 24" className="mx-auto size-12 text-brand-bright" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><path d="M5 5h14v4a3 3 0 0 0 0 6v4H5v-4a3 3 0 0 0 0-6V5Z" /><path d="M12 7v2M12 11v2M12 15v2" /></svg>
        <h2 id="spin-limit-title" className="mt-5 text-2xl font-bold tracking-tight">
          Seus giros acabaram
        </h2>
        <p className="mt-3 text-sm leading-6 text-white/55">
          Volte amanhã ou continue sem limites com o CineGiro Premium.
        </p>
        <Link
          to="/premium"
          className="mt-6 block w-full rounded-xl bg-brand px-5 py-3.5 text-sm font-bold text-white transition hover:bg-brand-bright focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          Conhecer o Premium
        </Link>
        <button
          type="button"
          onClick={onClose}
          autoFocus
          className="mt-2 w-full rounded-xl px-5 py-3 text-sm font-bold text-white/55 transition hover:bg-white/[0.05] hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2"
        >
          Agora não
        </button>
      </motion.div>
    </motion.div>
  )
}
