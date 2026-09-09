import { useCallback, useEffect, useRef, useState } from 'react'
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
  const scrollContainerRef = useRef<HTMLDivElement>(null)
  const [canScrollBackward, setCanScrollBackward] = useState(false)
  const [canScrollForward, setCanScrollForward] = useState(false)

  const updateScrollControls = useCallback(() => {
    const container = scrollContainerRef.current
    if (!container) return

    const maximumScroll = Math.max(0, container.scrollWidth - container.clientWidth)
    setCanScrollBackward(container.scrollLeft > 2)
    setCanScrollForward(maximumScroll > 2 && container.scrollLeft < maximumScroll - 2)
  }, [])

  const handleWheel = useCallback((event: WheelEvent) => {
    const container = scrollContainerRef.current
    if (!container || Math.abs(event.deltaY) <= Math.abs(event.deltaX)) return

    const maximumScroll = container.scrollWidth - container.clientWidth
    const canMoveInDirection = event.deltaY > 0
      ? container.scrollLeft < maximumScroll - 2
      : container.scrollLeft > 2

    if (!canMoveInDirection) return

    event.preventDefault()
    container.scrollLeft += event.deltaY
    updateScrollControls()
  }, [updateScrollControls])

  useEffect(() => {
    const container = scrollContainerRef.current
    if (!container) return

    const frame = window.requestAnimationFrame(updateScrollControls)
    const resizeObserver = typeof ResizeObserver === 'undefined'
      ? null
      : new ResizeObserver(updateScrollControls)

    resizeObserver?.observe(container)
    container.addEventListener('wheel', handleWheel, { passive: false })
    window.addEventListener('resize', updateScrollControls)

    return () => {
      window.cancelAnimationFrame(frame)
      resizeObserver?.disconnect()
      container.removeEventListener('wheel', handleWheel)
      window.removeEventListener('resize', updateScrollControls)
    }
  }, [handleWheel, loading, options.length, updateScrollControls])

  function scrollByPage(direction: -1 | 1) {
    const container = scrollContainerRef.current
    if (!container) return

    const distance = Math.max(180, container.clientWidth * 0.72)
    container.scrollTo({
      left: container.scrollLeft + direction * distance,
      behavior: 'smooth',
    })
    window.setTimeout(updateScrollControls, 350)
  }

  return (
    <fieldset className="min-w-0">
      <legend className="mb-2.5 px-1 text-[0.68rem] font-semibold uppercase tracking-[0.12em] text-white/60">
        {legend}
      </legend>

      <div className="relative min-w-0">
        {canScrollBackward ? (
          <button
            type="button"
            onClick={() => scrollByPage(-1)}
            className="absolute left-0 top-1/2 z-10 grid size-9 -translate-y-[calc(50%+0.25rem)] place-items-center rounded-full border border-white/15 bg-surface-raised/95 text-white shadow-xl backdrop-blur transition hover:border-white/30 hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
            aria-label={`Ver opções anteriores de ${legend}`}
          >
            <svg viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="m14.5 6-6 6 6 6" />
            </svg>
          </button>
        ) : null}

        <div
          ref={scrollContainerRef}
          onScroll={updateScrollControls}
          className="scrollbar-hidden flex snap-x scroll-px-10 gap-2 overflow-x-auto overscroll-x-contain px-1 pb-2"
          aria-label={`Opções de ${legend}`}
        >
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

        {canScrollForward ? (
          <button
            type="button"
            onClick={() => scrollByPage(1)}
            className="absolute right-0 top-1/2 z-10 grid size-9 -translate-y-[calc(50%+0.25rem)] place-items-center rounded-full border border-white/15 bg-surface-raised/95 text-white shadow-xl backdrop-blur transition hover:border-white/30 hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
            aria-label={`Ver próximas opções de ${legend}`}
          >
            <svg viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="m9.5 6 6 6-6 6" />
            </svg>
          </button>
        ) : null}
      </div>
    </fieldset>
  )
}
