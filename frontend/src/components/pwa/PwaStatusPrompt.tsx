import { AnimatePresence, motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import {
  activateWaitingServiceWorker,
  PWA_UPDATE_AVAILABLE_EVENT,
} from '../../pwa/registerServiceWorker'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

export const PWA_ENGAGEMENT_EVENT = 'reelz:pwa-engagement'

export function PwaStatusPrompt() {
  const [updateAvailable, setUpdateAvailable] = useState(false)
  const [installPrompt, setInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [installEligible, setInstallEligible] = useState(false)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    function handleUpdateAvailable() {
      setUpdateAvailable(true)
    }

    function handleInstallAvailable(event: Event) {
      event.preventDefault()
      setInstallPrompt(event as BeforeInstallPromptEvent)
    }

    function handleInstalled() {
      setInstallPrompt(null)
    }

    let engagementCount = 0
    function handleEngagement() {
      engagementCount += 1
      if (engagementCount >= 2) setInstallEligible(true)
    }

    window.addEventListener(PWA_UPDATE_AVAILABLE_EVENT, handleUpdateAvailable)
    window.addEventListener('beforeinstallprompt', handleInstallAvailable)
    window.addEventListener('appinstalled', handleInstalled)
    window.addEventListener(PWA_ENGAGEMENT_EVENT, handleEngagement)
    return () => {
      window.removeEventListener(PWA_UPDATE_AVAILABLE_EVENT, handleUpdateAvailable)
      window.removeEventListener('beforeinstallprompt', handleInstallAvailable)
      window.removeEventListener('appinstalled', handleInstalled)
      window.removeEventListener(PWA_ENGAGEMENT_EVENT, handleEngagement)
    }
  }, [])

  async function updateApplication() {
    setBusy(true)
    const activated = await activateWaitingServiceWorker()
    if (!activated) {
      setBusy(false)
      setUpdateAvailable(false)
    }
  }

  async function installApplication() {
    if (!installPrompt) return
    setBusy(true)
    await installPrompt.prompt()
    await installPrompt.userChoice
    setInstallPrompt(null)
    setBusy(false)
  }

  const visible = updateAvailable || (installPrompt !== null && installEligible)

  return (
    <AnimatePresence>
      {visible ? (
        <motion.aside
          key={updateAvailable ? 'update' : 'install'}
          role="status"
          aria-live="polite"
          initial={{ opacity: 0, y: 28, scale: 0.92 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 20, scale: 0.94 }}
          transition={{ type: 'spring', stiffness: 380, damping: 27 }}
          className="fixed inset-x-4 bottom-24 z-[60] mx-auto flex max-w-md items-center gap-3 rounded-xl border border-white/12 bg-surface-raised/95 p-3 shadow-2xl backdrop-blur-xl lg:bottom-6"
        >
          <span
            className="grid size-10 shrink-0 place-items-center rounded-lg bg-reel text-base font-bold text-white"
            aria-hidden="true"
          >
            {updateAvailable ? '↻' : '↓'}
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-bold text-paper">
              {updateAvailable ? 'Nova versão disponível' : 'Instale o Reelz'}
            </p>
            <p className="mt-0.5 text-xs leading-5 text-white/45">
              {updateAvailable
                ? 'Atualize para receber as últimas melhorias.'
                : 'Abra direto da tela inicial, como um app.'}
            </p>
          </div>
          <button
            type="button"
            disabled={busy}
            onClick={() => {
              if (updateAvailable) void updateApplication()
              else void installApplication()
            }}
            className="shrink-0 rounded-lg bg-paper px-3 py-2.5 text-xs font-bold text-canvas transition hover:bg-white disabled:cursor-wait disabled:opacity-55"
          >
            {busy ? 'Aguarde…' : updateAvailable ? 'Atualizar' : 'Instalar'}
          </button>
          <button
            type="button"
            onClick={() => {
              if (updateAvailable) setUpdateAvailable(false)
              else setInstallPrompt(null)
            }}
            aria-label="Fechar aviso"
            className="grid size-8 shrink-0 place-items-center rounded-lg text-white/55 transition hover:bg-white/5 hover:text-white"
          >
            <span aria-hidden="true">×</span>
          </button>
        </motion.aside>
      ) : null}
    </AnimatePresence>
  )
}
