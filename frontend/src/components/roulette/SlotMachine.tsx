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
        className="relative h-52 w-44 overflow-hidden rounded-[2rem] border border-white/15 bg-surface p-3 shadow-2xl"
      >
        <div className="pointer-events-none absolute inset-x-0 top-0 z-10 h-16 bg-gradient-to-b from-surface to-transparent" />
        <div className="pointer-events-none absolute inset-x-0 bottom-0 z-10 h-16 bg-gradient-to-t from-surface to-transparent" />
        <div className="pointer-events-none absolute inset-x-3 top-1/2 z-20 h-12 -translate-y-1/2 rounded-xl border border-reel/50 bg-reel/10" />

        <motion.div
          animate={{ y: [0, -288] }}
          transition={{ duration: 0.72, repeat: Infinity, ease: 'linear' }}
          className="flex flex-col gap-2"
          aria-hidden="true"
        >
          {[...reelItems, ...reelItems].map((item, index) => (
            <div
              key={`${item}-${index}`}
              className="grid h-10 shrink-0 place-items-center rounded-xl bg-white/[0.055] text-[10px] font-black tracking-[0.16em] text-white/65"
            >
              {item}
            </div>
          ))}
        </motion.div>
      </motion.div>

      <motion.p
        animate={{ opacity: [0.4, 1, 0.4] }}
        transition={{ duration: 1.1, repeat: Infinity }}
        className="mt-5 text-sm font-bold text-white/65"
      >
        Procurando a sessão perfeita…
      </motion.p>
    </motion.div>
  )
}
