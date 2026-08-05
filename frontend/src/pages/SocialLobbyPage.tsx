import { useEffect, useState, type FormEvent } from 'react'
import { motion } from 'framer-motion'
import { useNavigate, useSearchParams } from 'react-router'
import { ReelzLogo } from '../components/brand/ReelzLogo'
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
    <main className="min-h-svh bg-canvas px-4 pt-5 pb-28 text-white sm:px-8">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_50%_10%,rgba(139,92,246,.17),transparent_34%)]" />
      <header className="relative mx-auto flex max-w-4xl items-center justify-between">
        <ReelzLogo />
        <span className="rounded-full border border-violet-300/15 bg-violet-300/[0.06] px-3 py-2 text-[10px] font-black tracking-wider text-violet-100 uppercase">
          Juntos
        </span>
      </header>

      <div className="relative mx-auto max-w-4xl pt-10">
        <p className="text-xs font-black tracking-[.2em] text-violet-300 uppercase">Decisão compartilhada</p>
        <h1 className="mt-2 max-w-2xl text-4xl font-black tracking-[-.05em] sm:text-5xl">
          Uma roleta para todo mundo.
        </h1>
        <p className="mt-4 max-w-xl leading-7 text-white/50">
          O Reelz cruza os streamings em comum e remove os filmes que qualquer participante já viu.
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
                className={`rounded-[1.75rem] border p-6 text-left transition disabled:opacity-50 ${
                  selected
                    ? 'border-violet-300/30 bg-violet-300/[0.1] shadow-[0_20px_70px_rgba(139,92,246,.12)]'
                    : 'border-white/10 bg-white/[0.035] hover:border-white/20'
                }`}
              >
                <span className="text-3xl" aria-hidden="true">{type === 'COUPLE' ? '💞' : '🍿'}</span>
                <span className="mt-5 block text-xl font-black">Modo {roomLabel(type).toLowerCase()}</span>
                <span className="mt-2 block text-sm leading-6 text-white/45">
                  {type === 'COUPLE'
                    ? 'Você e mais uma pessoa, sem perder tempo comparando catálogos.'
                    : 'Até 8 participantes na mesma escolha de filme.'}
                </span>
                <span className="mt-5 block text-xs font-black text-violet-200">
                  {pendingAction === type ? 'Criando…' : 'Criar sala →'}
                </span>
              </motion.button>
            )
          })}
        </section>

        <form onSubmit={handleJoin} className="mt-5 flex gap-2 rounded-[1.5rem] border border-white/10 bg-white/[0.025] p-3">
          <label htmlFor="invite-code" className="sr-only">Código do convite</label>
          <input
            id="invite-code"
            value={inviteCode}
            onChange={(event) => setInviteCode(event.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 8))}
            placeholder="CÓDIGO DO CONVITE"
            autoComplete="off"
            className="min-w-0 flex-1 bg-transparent px-3 text-sm font-black tracking-[.14em] outline-none placeholder:text-white/25"
          />
          <button
            type="submit"
            disabled={pendingAction !== null}
            className="rounded-xl bg-white px-4 py-3 text-xs font-black text-canvas transition hover:bg-violet-100 disabled:opacity-50"
          >
            {pendingAction === 'JOIN' ? 'Entrando…' : 'Entrar'}
          </button>
        </form>

        <section className="mt-10">
          <h2 className="text-sm font-black text-white/75">Suas salas</h2>
          {loading ? <div className="mt-4 h-24 animate-pulse rounded-3xl bg-white/[0.04]" /> : null}
          {!loading && rooms.length === 0 ? (
            <p className="mt-3 text-sm text-white/35">Você ainda não participou de nenhuma sala.</p>
          ) : null}
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            {rooms.map((room) => (
              <button
                key={room.id}
                type="button"
                onClick={() => navigate(`/social/rooms/${room.id}`)}
                className="flex items-center justify-between rounded-2xl border border-white/8 bg-white/[0.025] p-4 text-left transition hover:border-violet-300/25"
              >
                <span>
                  <span className="block text-sm font-black">{roomLabel(room.type)}</span>
                  <span className="mt-1 block text-xs text-white/35">
                    {room.memberCount}/{room.capacity} pessoas · {room.lastSpinNumber} giros
                  </span>
                </span>
                <span className={room.status === 'OPEN' ? 'text-emerald-300' : 'text-white/25'}>
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
