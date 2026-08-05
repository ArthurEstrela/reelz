import { useEffect, useState, type FormEvent } from 'react'
import { motion } from 'framer-motion'
import { useNavigate, useSearchParams } from 'react-router'
import { AppHeader } from '../components/navigation/AppHeader'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { createSocialRoom, joinSocialRoom, listSocialRooms } from '../services/socialService'
import type { SocialRoomSummary, SocialRoomType } from '../types/social'
import { getApiErrorMessage } from '../utils/apiError'

function roomLabel(type: SocialRoomType) {
  return type === 'COUPLE' ? 'Casal' : 'Grupo'
}

export function SocialLobbyPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const suggestedMode = searchParams.get('mode') === 'GROUP' ? 'GROUP' : 'COUPLE'
  const [rooms, setRooms] = useState<SocialRoomSummary[]>([])
  const [inviteCode, setInviteCode] = useState('')
  const [loading, setLoading] = useState(true)
  const [pendingAction, setPendingAction] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    listSocialRooms()
      .then((response) => {
        if (!cancelled) setRooms(response)
      })
      .catch((requestError) => {
        if (!cancelled) setError(getApiErrorMessage(requestError, 'Não foi possível carregar suas salas.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function handleCreate(type: SocialRoomType) {
    setPendingAction(type)
    setError(null)
    try {
      const room = await createSocialRoom(type)
      navigate(`/social/rooms/${room.id}`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível criar a sala.'))
      setPendingAction(null)
    }
  }

  async function handleJoin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedCode = inviteCode.trim().toUpperCase()
    if (normalizedCode.length !== 8) {
      setError('Digite os 8 caracteres do convite.')
      return
    }
    setPendingAction('JOIN')
    setError(null)
    try {
      const room = await joinSocialRoom(normalizedCode)
      navigate(`/social/rooms/${room.id}`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível entrar nessa sala.'))
      setPendingAction(null)
    }
  }

  return (
    <main className="min-h-svh bg-canvas px-4 pt-5 pb-28 text-paper sm:px-8 lg:pb-12">
      <div className="pointer-events-none fixed inset-x-0 top-0 h-[32rem] bg-[radial-gradient(circle_at_50%_5%,rgba(233,54,69,.12),transparent_40%)]" />
      <AppHeader />

      <div className="relative mx-auto max-w-4xl pt-10">
        <p className="reelz-kicker">Decisão compartilhada</p>
        <h1 className="mt-2 max-w-2xl text-4xl font-extrabold tracking-[-.045em] sm:text-5xl">
          Todo mundo dá o pitaco.
        </h1>
        <p className="mt-4 max-w-xl leading-7 text-white/60">
          Cada pessoa escolhe o que topa. O Reelz encontra um filme disponível para o grupo inteiro.
        </p>

        {error ? (
          <motion.p
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            role="alert"
            className="mt-6 rounded-2xl border border-red-300/15 bg-red-300/[0.06] px-4 py-3 text-sm font-bold text-red-100"
          >
            {error}
          </motion.p>
        ) : null}

        <section className="mt-8 grid gap-4 sm:grid-cols-2" aria-label="Criar uma sala">
          {(['COUPLE', 'GROUP'] as const).map((type) => {
            const selected = suggestedMode === type
            return (
              <motion.button
                key={type}
                type="button"
                whileTap={{ scale: 0.97 }}
                onClick={() => void handleCreate(type)}
                disabled={pendingAction !== null}
                className={`rounded-2xl border p-6 text-left transition disabled:opacity-50 ${
                  selected
                    ? 'border-reel/45 bg-reel/[0.08] shadow-[0_20px_60px_rgba(233,54,69,.1)]'
                    : 'border-white/10 bg-white/[0.025] hover:border-white/25'
                }`}
              >
                <svg viewBox="0 0 24 24" className="size-8 text-reel-bright" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" aria-hidden="true">
                  {type === 'COUPLE' ? <><circle cx="8" cy="9" r="3" /><circle cx="16" cy="9" r="3" /><path d="M3.5 19c.4-3.6 1.9-5.3 4.5-5.3s4.1 1.7 4.5 5.3M11.5 19c.4-3.6 1.9-5.3 4.5-5.3s4.1 1.7 4.5 5.3" /></> : <><circle cx="12" cy="7" r="2.5" /><circle cx="6" cy="10" r="2" /><circle cx="18" cy="10" r="2" /><path d="M7 19c.3-4 2-6 5-6s4.7 2 5 6M2.5 19c.2-2.7 1.4-4.3 3.5-4.3M21.5 19c-.2-2.7-1.4-4.3-3.5-4.3" /></>}
                </svg>
                <span className="mt-5 block text-xl font-bold">Modo {roomLabel(type).toLowerCase()}</span>
                <span className="mt-2 block text-sm leading-6 text-white/60">
                  {type === 'COUPLE'
                    ? 'Você e mais uma pessoa, sem perder tempo comparando catálogos.'
                    : 'Até 8 participantes na mesma escolha de filme.'}
                </span>
                <span className="mt-5 block text-xs font-semibold text-reel-bright">
                  {pendingAction === type ? 'Criando…' : 'Criar sala →'}
                </span>
              </motion.button>
            )
          })}
        </section>

        <form onSubmit={handleJoin} className="mt-5 flex gap-2 rounded-xl border border-white/10 bg-white/[0.025] p-2 focus-within:border-reel/55 focus-within:ring-4 focus-within:ring-reel/8">
          <label htmlFor="invite-code" className="sr-only">Código do convite</label>
          <input
            id="invite-code"
            value={inviteCode}
            onChange={(event) => setInviteCode(event.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 8))}
            placeholder="CÓDIGO DO CONVITE"
            autoComplete="off"
            className="min-w-0 flex-1 bg-transparent px-3 text-sm font-semibold tracking-[.12em] outline-none placeholder:text-white/45"
          />
          <button
            type="submit"
            disabled={pendingAction !== null}
            className="rounded-lg bg-paper px-4 py-3 text-xs font-bold text-canvas transition hover:bg-white disabled:opacity-50"
          >
            {pendingAction === 'JOIN' ? 'Entrando…' : 'Entrar'}
          </button>
        </form>

        <section className="mt-10">
          <h2 className="text-sm font-semibold text-white/80">Suas salas</h2>
          {loading ? <div className="mt-4 h-24 animate-pulse rounded-3xl bg-white/[0.04]" /> : null}
          {!loading && rooms.length === 0 ? (
            <p className="mt-3 text-sm text-white/55">Você ainda não participou de nenhuma sala.</p>
          ) : null}
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            {rooms.map((room) => (
              <button
                key={room.id}
                type="button"
                onClick={() => navigate(`/social/rooms/${room.id}`)}
                className="flex items-center justify-between rounded-xl border border-white/10 bg-white/[0.025] p-4 text-left transition hover:border-reel/35"
              >
                <span>
                  <span className="block text-sm font-semibold">{roomLabel(room.type)}</span>
                  <span className="mt-1 block text-xs text-white/55">
                    {room.memberCount}/{room.capacity} pessoas · {room.lastSpinNumber} giros
                  </span>
                </span>
                <span className={room.status === 'OPEN' ? 'text-emerald-300' : 'text-white/50'}>
                  {room.status === 'OPEN' ? 'Aberta →' : 'Encerrada'}
                </span>
              </button>
            ))}
          </div>
        </section>
      </div>
      <BottomNavigation />
    </main>
  )
}
