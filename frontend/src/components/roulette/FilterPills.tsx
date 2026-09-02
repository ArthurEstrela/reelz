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
  loading?: boolean
  disabled?: boolean
}

export function FilterPills<T extends string | number>({
  legend,
  options,
  selectedValues,
  onToggle,
  loading = false,
  disabled = false,
}: FilterPillsProps<T>) {
  return (
    <fieldset className="min-w-0">
      <legend className="mb-2.5 px-1 text-[0.68rem] font-semibold uppercase tracking-[0.12em] text-white/60">
        {legend}
      </legend>

      <div className="scrollbar-hidden flex snap-x gap-2 overflow-x-auto px-1 pb-2">
        {loading ? (
          <div className="flex gap-2" role="status" aria-label={`Carregando ${legend.toLowerCase()}`}>
            <span className="sr-only">Carregando filtros…</span>
            {[5.5, 7, 6, 8].map((width, index) => (
              <motion.span
                key={width}
                animate={{ opacity: [0.25, 0.55, 0.25] }}
                transition={{ duration: 1.1, delay: index * 0.08, repeat: Infinity }}
                className="h-10 shrink-0 rounded-full bg-white/10"
                style={{ width: `${width}rem` }}
                aria-hidden="true"
              />
            ))}
          </div>
        ) : null}

        {!loading && options.length === 0 ? (
          <p className="px-1 py-2 text-sm text-white/55">Nenhuma opção disponível.</p>
        ) : null}

        {!loading ? options.map((option) => {
          const selected = selectedValues.includes(option.value)

          return (
            <motion.button
              layout
              key={option.value}
              type="button"
              disabled={disabled || option.disabled}
              aria-pressed={selected}
              title={option.disabled ? 'Configure este filtro no ambiente para habilitá-lo.' : undefined}
              onClick={() => onToggle(option.value)}
              whileTap={disabled || option.disabled ? undefined : { scale: 0.94 }}
              animate={selected ? { scale: 1.03, y: -1 } : { scale: 1, y: 0 }}
              transition={{ type: 'spring', stiffness: 420, damping: 28 }}
              className={`shrink-0 snap-start rounded-xl border px-3.5 py-2.5 text-sm font-semibold outline-none transition-colors focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-canvas disabled:cursor-not-allowed disabled:opacity-40 ${
                selected
                  ? 'border-brand bg-brand text-white shadow-[0_8px_24px_rgba(233,54,69,.2)]'
                  : 'border-white/12 bg-transparent text-white/65 hover:border-white/25 hover:bg-white/[0.05] hover:text-white'
              }`}
            >
              {option.emoji ? <span className="mr-1.5" aria-hidden="true">{option.emoji}</span> : null}
              {option.label}
            </motion.button>
          )
        }) : null}
      </div>
    </fieldset>
  )
}
