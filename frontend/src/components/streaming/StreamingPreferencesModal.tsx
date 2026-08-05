import { useRef, useState } from 'react'
import { motion } from 'framer-motion'
import type { CatalogItem } from '../../types/catalog'
import { getApiErrorMessage } from '../../utils/apiError'
import { useDialogFocus } from '../../hooks/useDialogFocus'

interface StreamingPreferencesModalProps {
  providers: CatalogItem[]
  selectedProviderIds: readonly string[]
  onClose: () => void
  onSave: (providerIds: string[]) => Promise<void>
}

export function StreamingPreferencesModal({
  providers,
  selectedProviderIds,
  onClose,
  onSave,
}: StreamingPreferencesModalProps) {
  const [selection, setSelection] = useState(() => new Set(selectedProviderIds))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const dialogRef = useRef<HTMLElement>(null)

  useDialogFocus(true, dialogRef, onClose, saving)

  function toggle(providerId: string) {
    setSelection((current) => {
      const next = new Set(current)
      if (next.has(providerId)) next.delete(providerId)
      else next.add(providerId)
      return next
    })
  }

  async function save() {
    setSaving(true)
    setError(null)
    try {
      const providerIds = providers
        .map((provider) => provider.id)
        .filter((providerId) => selection.has(providerId))
      await onSave(providerIds)
    } catch (saveError) {
      setError(getApiErrorMessage(saveError, 'Não foi possível salvar seus streamings. Tente novamente.'))
      setSaving(false)
    }
  }

  return (
    <motion.div
      className="fixed inset-0 z-[60] flex items-end justify-center bg-black/75 p-3 backdrop-blur-sm sm:items-center"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !saving) onClose()
      }}
    >
      <motion.section
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="streaming-preferences-title"
        initial={{ opacity: 0, y: 80, scale: 0.92 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 50, scale: 0.95 }}
        transition={{ type: 'spring', stiffness: 340, damping: 28 }}
        className="max-h-[85svh] w-full max-w-lg overflow-hidden rounded-2xl border border-white/10 bg-surface-raised shadow-2xl"
      >
        <header className="border-b border-white/8 px-5 py-5 sm:px-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="reelz-kicker">Sua assinatura, seu catálogo</p>
              <h2 id="streaming-preferences-title" className="mt-1 text-2xl font-bold tracking-tight">
                Meus streamings
              </h2>
              <p className="mt-2 text-sm leading-6 text-white/50">
                Marque todos que você possui. No plano Free, você continua escolhendo um por giro.
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              aria-label="Fechar preferências"
              className="grid size-10 shrink-0 place-items-center rounded-full bg-white/[0.06] text-xl text-white/55 transition hover:bg-white/10 hover:text-white disabled:opacity-40"
            >
              ×
            </button>
          </div>
        </header>

        <div className="max-h-[52svh] overflow-y-auto p-4 sm:p-6">
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3" role="group" aria-label="Serviços disponíveis">
            {providers.map((provider) => {
              const selected = selection.has(provider.id)
              return (
                <motion.button
                  key={provider.id}
                  type="button"
                  aria-pressed={selected}
                  onClick={() => toggle(provider.id)}
                  whileTap={{ scale: 0.96 }}
                  animate={selected ? { scale: 1, y: -1 } : { scale: 1, y: 0 }}
                  transition={{ type: 'spring', stiffness: 420, damping: 28 }}
                  className={`min-h-16 rounded-xl border px-3 py-3 text-left text-sm font-semibold transition-colors ${
                    selected
                      ? 'border-reel bg-reel/15 text-white shadow-[0_8px_30px_rgba(255,60,72,.14)]'
                      : 'border-white/8 bg-white/[0.035] text-white/55 hover:border-white/15 hover:text-white'
                  }`}
                >
                  <span className="flex items-center gap-2">
                    <span
                      aria-hidden="true"
                      className={`grid size-5 shrink-0 place-items-center rounded-md border text-xs ${
                        selected ? 'border-reel bg-reel text-white' : 'border-white/15 text-transparent'
                      }`}
                    >
                      ✓
                    </span>
                    {provider.name}
                  </span>
                </motion.button>
              )
            })}
          </div>

          {error ? <p role="alert" className="mt-4 text-sm font-bold text-red-200">{error}</p> : null}
        </div>

        <footer className="flex items-center justify-between gap-3 border-t border-white/8 px-5 py-4 sm:px-6">
          <span className="text-xs font-medium text-white/55">
            {selection.size === 0 ? 'Nenhum selecionado' : `${selection.size} selecionado${selection.size === 1 ? '' : 's'}`}
          </span>
          <motion.button
            type="button"
            onClick={() => void save()}
            disabled={saving}
            whileTap={saving ? undefined : { scale: 0.96 }}
            className="rounded-xl bg-reel px-5 py-3 text-sm font-bold text-white shadow-[0_12px_32px_rgba(233,54,69,.2)] disabled:cursor-wait disabled:opacity-55"
          >
            {saving ? 'Salvando…' : 'Salvar streamings'}
          </motion.button>
        </footer>
      </motion.section>
    </motion.div>
  )
}
