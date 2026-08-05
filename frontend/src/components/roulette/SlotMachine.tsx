import { motion } from 'framer-motion'

const reelItems = ['COMÉDIA', 'AÇÃO', 'DRAMA', 'TERROR', 'FICÇÃO', 'SUSPENSE']

export function SlotMachine() {
  return (
    <motion.div
      key="slot-machine"
      initial={{ opacity: 0, scale: 0.82 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.72, filter: 'blur(10px)' }}
      transition={{ type: 'spring', bounce: 0.42, duration: 0.65 }}
      className="flex flex-col items-center"
      role="status"
      aria-live="polite"
    >
      <motion.div
        animate={{ scale: [1, 1.05, 1], boxShadow: ['0 0 0 rgba(255,60,72,0)', '0 0 70px rgba(255,60,72,.35)', '0 0 0 rgba(255,60,72,0)'] }}
        transition={{ duration: 0.8, repeat: Infinity, ease: 'easeInOut' }}
        className="relative h-52 w-48 overflow-hidden rounded-xl border border-white/15 bg-surface px-5 py-3 shadow-2xl before:absolute before:inset-y-0 before:left-1 before:w-2 before:bg-[repeating-linear-gradient(to_bottom,transparent_0_8px,rgba(255,255,255,.13)_8px_13px,transparent_13px_21px)] after:absolute after:inset-y-0 after:right-1 after:w-2 after:bg-[repeating-linear-gradient(to_bottom,transparent_0_8px,rgba(255,255,255,.13)_8px_13px,transparent_13px_21px)]"
      >
        <div className="pointer-events-none absolute inset-x-0 top-0 z-10 h-16 bg-gradient-to-b from-surface to-transparent" />
        <div className="pointer-events-none absolute inset-x-0 bottom-0 z-10 h-16 bg-gradient-to-t from-surface to-transparent" />
        <div className="pointer-events-none absolute inset-x-4 top-1/2 z-20 h-12 -translate-y-1/2 border-y border-reel/60 bg-reel/8" />

        <motion.div
          animate={{ y: [0, -288] }}
          transition={{ duration: 0.72, repeat: Infinity, ease: 'linear' }}
          className="flex flex-col gap-2"
          aria-hidden="true"
        >
          {[...reelItems, ...reelItems].map((item, index) => (
            <div
              key={`${item}-${index}`}
              className="grid h-10 shrink-0 place-items-center border-b border-white/[0.06] text-[10px] font-semibold tracking-[0.13em] text-white/70"
            >
              {item}
            </div>
          ))}
        </motion.div>
      </motion.div>

      <motion.p
        animate={{ opacity: [0.4, 1, 0.4] }}
        transition={{ duration: 1.1, repeat: Infinity }}
        className="mt-5 text-sm font-semibold text-white/70"
      >
        Procurando a sessão perfeita…
      </motion.p>
    </motion.div>
  )
}
