import { motion } from 'framer-motion'

export interface PillOption<T extends string | number> {
  value: T
  label: string
  emoji?: string
  disabled?: boolean
}

interface FilterPillsProps<T extends string | number> {
  legend: string
  options: PillOption<T>[]
  selectedValues: readonly T[]
  onToggle: (value: T) => void
}

export function FilterPills<T extends string | number>({
  legend,
  options,
  selectedValues,
  onToggle,
}: FilterPillsProps<T>) {
  return (
    <fieldset className="min-w-0">
      <legend className="mb-3 px-1 text-xs font-extrabold uppercase tracking-[0.18em] text-white/45">
        {legend}
      </legend>

      <div className="scrollbar-hidden flex snap-x gap-2 overflow-x-auto px-1 pb-2">
        {options.map((option) => {
          const selected = selectedValues.includes(option.value)

          return (
            <motion.button
              layout
              key={option.value}
              type="button"
              disabled={option.disabled}
              aria-pressed={selected}
              title={option.disabled ? 'Configure este filtro no ambiente para habilitá-lo.' : undefined}
              onClick={() => onToggle(option.value)}
              whileTap={option.disabled ? undefined : { scale: 0.94 }}
              animate={selected ? { scale: 1.03, y: -1 } : { scale: 1, y: 0 }}
              transition={{ type: 'spring', stiffness: 420, damping: 28 }}
              className={`shrink-0 snap-start rounded-full border px-4 py-2.5 text-sm font-bold outline-none transition-colors focus-visible:ring-2 focus-visible:ring-reel focus-visible:ring-offset-2 focus-visible:ring-offset-canvas disabled:cursor-not-allowed disabled:opacity-35 ${
                selected
                  ? 'border-reel bg-reel text-white shadow-[0_8px_28px_rgba(255,60,72,.26)]'
                  : 'border-white/10 bg-white/[0.045] text-white/65 hover:border-white/20 hover:bg-white/[0.075] hover:text-white'
              }`}
            >
              {option.emoji ? <span className="mr-1.5" aria-hidden="true">{option.emoji}</span> : null}
              {option.label}
            </motion.button>
          )
        })}
      </div>
    </fieldset>
  )
}
