import { AnimatePresence, motion } from 'framer-motion'
import { useEffect, useState, type FormEvent } from 'react'
import { submitBetaFeedback } from '../../services/feedbackService'
import { getApiErrorMessage } from '../../utils/apiError'

interface FeedbackDialogProps {
  open: boolean
  onClose: () => void
}

const scoreLabels = ['Nada', 'Pouco', 'Mais ou menos', 'Muito', 'Demais']

export function FeedbackDialog({ open, onClose }: FeedbackDialogProps) {
  const [score, setScore] = useState<number | null>(null)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)

  useEffect(() => {
    if (!open) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !loading) onClose()
    }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [loading, onClose, open])

  const close = () => {
    if (loading) return
    setScore(null)
    setMessage('')
    setError(null)
    setSubmitted(false)
    onClose()
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (!score) {
      setError('Escolha uma nota de 1 a 5.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      await submitBetaFeedback({ score, message: message.trim() || undefined })
      setSubmitted(true)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível enviar agora. Tente novamente.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AnimatePresence>
      {open ? (
        <motion.div
          className="fixed inset-0 z-[70] flex items-end justify-center bg-black/75 p-3 backdrop-blur-sm sm:items-center"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onMouseDown={close}
        >
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-labelledby="feedback-title"
            className="w-full max-w-md rounded-[2rem] border border-white/10 bg-[#111116] p-5 shadow-2xl"
            initial={{ y: 80, scale: 0.94 }}
            animate={{ y: 0, scale: 1 }}
            exit={{ y: 80, scale: 0.94 }}
            transition={{ type: 'spring', stiffness: 340, damping: 28 }}
            onMouseDown={(event) => event.stopPropagation()}
          >
            {submitted ? (
              <div className="py-5 text-center">
                <span className="text-4xl" aria-hidden="true">🎬</span>
                <h2 id="feedback-title" className="mt-3 text-2xl font-black">Feedback recebido</h2>
                <p className="mt-2 text-sm leading-6 text-white/50">
                  Valeu por ajudar a deixar a próxima sessão mais certeira.
                </p>
                <button type="button" onClick={close} className="mt-6 w-full rounded-2xl bg-white px-5 py-3 font-black text-black">
                  Fechar
                </button>
              </div>
            ) : (
              <form onSubmit={submit}>
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-[10px] font-black tracking-[.16em] text-reel uppercase">Beta Reelz</p>
                    <h2 id="feedback-title" className="mt-1 text-xl font-black">O Reelz ajudou você a decidir?</h2>
                  </div>
                  <button type="button" onClick={close} aria-label="Fechar feedback" className="rounded-full bg-white/5 px-3 py-2 text-white/50">×</button>
                </div>

                <fieldset className="mt-6">
                  <legend className="sr-only">Nota de 1 a 5</legend>
                  <div className="grid grid-cols-5 gap-2">
                    {scoreLabels.map((label, index) => {
                      const value = index + 1
                      return (
                        <button
                          key={value}
                          type="button"
                          aria-label={`${value}: ${label}`}
                          aria-pressed={score === value}
                          onClick={() => setScore(value)}
                          className={`rounded-2xl py-3 text-lg font-black transition ${score === value ? 'bg-reel text-black' : 'bg-white/5 text-white/45 hover:bg-white/10'}`}
                        >
                          {value}
                        </button>
                      )
                    })}
                  </div>
                  <p className="mt-2 flex justify-between text-[10px] text-white/30"><span>Nada</span><span>Demais</span></p>
                </fieldset>

                <label className="mt-5 block text-xs font-bold text-white/60" htmlFor="beta-feedback-message">
                  O que mais pesou? <span className="font-normal text-white/30">(opcional)</span>
                </label>
                <textarea
                  id="beta-feedback-message"
                  value={message}
                  maxLength={1000}
                  onChange={(event) => setMessage(event.target.value)}
                  placeholder="Ex.: encontrei rápido, mas o link do streaming estava errado..."
                  className="mt-2 min-h-28 w-full resize-none rounded-2xl border border-white/8 bg-white/[0.035] p-4 text-sm text-white outline-none placeholder:text-white/20 focus:border-reel/50"
                />
                <p className="mt-1 text-right text-[10px] text-white/25">{message.length}/1000</p>
                <p className="mt-1 text-[10px] text-white/25">Não inclua senha, telefone ou outros dados pessoais.</p>

                {error ? <p role="alert" className="mt-3 text-sm font-bold text-red-200">{error}</p> : null}

                <button
                  type="submit"
                  disabled={loading}
                  className="mt-5 w-full rounded-2xl bg-white px-5 py-3 font-black text-black transition active:scale-[.98] disabled:opacity-50"
                >
                  {loading ? 'Enviando...' : 'Enviar feedback'}
                </button>
              </form>
            )}
          </motion.div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  )
}
