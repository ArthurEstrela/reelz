import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { useNavigate, useParams } from 'react-router'
import { getProductSessionId } from '../analytics/productSession'
import { AppHeader } from '../components/navigation/AppHeader'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { FilterPills, type PillOption } from '../components/roulette/FilterPills'
import { SlotMachine } from '../components/roulette/SlotMachine'
import { GENRE_OPTIONS } from '../config/rouletteFilters'
import { useAuth } from '../hooks/useAuth'
import { useAchievements } from '../hooks/useAchievements'
import { getVibes } from '../services/catalogService'
import { trackProductEvent } from '../services/analyticsService'
import { markMovieAsWatched } from '../services/historyService'
import { getTodayUsage } from '../services/rouletteService'
import {
  getSocialRoom,
  leaveSocialRoom,
  spinSocialRoom,
  updateSocialPreference,
} from '../services/socialService'
import type { RouletteMovie, SpinQuota } from '../types/roulette'
import type { SocialRoom } from '../types/social'
import { getApiErrorMessage } from '../utils/apiError'
import { resolveCatalogImageUrl } from '../utils/catalogImage'

function wait(milliseconds: number) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds))
}

export function SocialRoomPage() {
  const { roomId = '' } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { refreshAchievements } = useAchievements()
  const productSessionId = useMemo(() => getProductSessionId(), [])
  const mounted = useRef(true)
  const spinningRef = useRef(false)
  const preferenceDraftDirty = useRef(false)
  const [room, setRoom] = useState<SocialRoom | null>(null)
  const [quota, setQuota] = useState<SpinQuota | null>(null)
  const [vibes, setVibes] = useState<PillOption<string>[]>([])
  const [selectedProviders, setSelectedProviders] = useState<string[]>([])
  const [selectedGenres, setSelectedGenres] = useState<number[]>([])
  const [selectedVibe, setSelectedVibe] = useState<string | null>(null)
  const [movie, setMovie] = useState<RouletteMovie | null>(null)
  const [loading, setLoading] = useState(true)
  const [spinning, setSpinning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [toast, setToast] = useState<string | null>(null)
  const [leaving, setLeaving] = useState(false)
  const [savingPreference, setSavingPreference] = useState(false)

  const applyRoom = useCallback((nextRoom: SocialRoom) => {
    setRoom(nextRoom)
    setMovie(nextRoom.lastMovie)
    const currentMember = nextRoom.members.find((member) => member.userId === user?.id)
    if (currentMember && !preferenceDraftDirty.current) {
      setSelectedGenres(currentMember.selectedGenreIds)
      setSelectedVibe(currentMember.selectedVibeId)
    }
    const availableIds = new Set(nextRoom.commonProviders.map((provider) => provider.id))
    setSelectedProviders((current) => {
      const valid = current.filter((id) => availableIds.has(id))
      if (valid.length > 0) return valid
      return nextRoom.commonProviders[0] ? [nextRoom.commonProviders[0].id] : []
    })
  }, [user?.id])

  useEffect(() => {
    mounted.current = true
    Promise.all([getSocialRoom(roomId), getTodayUsage(), getVibes()])
      .then(([nextRoom, nextQuota, nextVibes]) => {
        if (!mounted.current) return
        applyRoom(nextRoom)
        setQuota(nextQuota)
        setVibes(nextVibes.map((vibe) => ({ value: vibe.id, label: vibe.name })))
      })
      .catch((requestError) => {
        if (mounted.current) setError(getApiErrorMessage(requestError, 'Não foi possível abrir esta sala.'))
      })
      .finally(() => {
        if (mounted.current) setLoading(false)
      })

    return () => {
      mounted.current = false
    }
  }, [applyRoom, roomId])

  useEffect(() => {
    if (!roomId || loading || error) return
    const interval = window.setInterval(() => {
      getSocialRoom(roomId)
        .then((nextRoom) => {
          if (mounted.current && !spinningRef.current) applyRoom(nextRoom)
        })
        .catch(() => undefined)
    }, 3_000)
    return () => window.clearInterval(interval)
  }, [applyRoom, error, loading, roomId, spinning])

  useEffect(() => {
    if (!toast) return
    const timeout = window.setTimeout(() => setToast(null), 4_000)
    return () => window.clearTimeout(timeout)
  }, [toast])

  function toggleProvider(providerId: string) {
    setSelectedProviders((current) => {
      if (current.includes(providerId)) return current.filter((id) => id !== providerId)
      return quota?.unlimited ? [...current, providerId] : [providerId]
    })
  }

  function toggleGenre(genreId: number) {
    preferenceDraftDirty.current = true
    setSelectedGenres((current) => {
      if (current.includes(genreId)) return current.filter((id) => id !== genreId)
      if (current.length >= 3) {
        setToast('Escolha no máximo 3 gêneros.')
        return current
      }
      return [...current, genreId]
    })
  }

  function toggleVibe(vibeId: string) {
    preferenceDraftDirty.current = true
    setSelectedVibe((current) => current === vibeId ? null : vibeId)
  }

  async function savePreference(ready: boolean) {
    if (!room || savingPreference) return
    if (ready && selectedGenres.length === 0 && selectedVibe === null) {
      setToast('Escolha pelo menos um gênero ou uma vibe.')
      return
    }
    setSavingPreference(true)
    setError(null)
    try {
      const nextRoom = await updateSocialPreference(room.id, {
        genreIds: selectedGenres,
        vibeId: selectedVibe,
        ready,
      })
      preferenceDraftDirty.current = false
      applyRoom(nextRoom)
      setToast(ready ? 'Palpite confirmado. Agora é só esperar todo mundo.' : 'Você pode alterar seu palpite.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível salvar seu palpite.'))
    } finally {
      setSavingPreference(false)
    }
  }

  async function handleSpin() {
    if (!room || spinning || selectedProviders.length === 0) return
    spinningRef.current = true
    setSpinning(true)
    setMovie(null)
    setError(null)
    const minimumAnimation = wait(2_000)
    try {
      const response = await spinSocialRoom(room.id, {
        idempotencyKey: crypto.randomUUID(),
        providerIds: selectedProviders,
        genreId: null,
        vibeId: null,
        sessionId: productSessionId,
      })
      await minimumAnimation
      applyRoom(response.room)
      setMovie(response.movie)
      setQuota(response.quota)
      void refreshAchievements()
    } catch (requestError) {
      await minimumAnimation
      setError(getApiErrorMessage(requestError, 'A roleta compartilhada não conseguiu girar.'))
    } finally {
      spinningRef.current = false
      setSpinning(false)
    }
  }

  async function handleCopyInvite() {
    if (!room) return
    const inviteUrl = `${window.location.origin}/social/join/${room.inviteCode}`
    try {
      await navigator.clipboard.writeText(inviteUrl)
      setToast('Link de convite copiado!')
    } catch {
      setToast(`Código do convite: ${room.inviteCode}`)
    }
  }

  async function handleWatched() {
    if (!movie) return
    try {
      await markMovieAsWatched(movie.tmdbId)
      void refreshAchievements()
      setToast('Filme adicionado à sua coleção.')
    } catch (requestError) {
      setToast(getApiErrorMessage(requestError, 'Não foi possível marcar o filme como visto.'))
    }
  }

  async function handleLeave() {
    if (!room || leaving) return
    setLeaving(true)
    try {
      await leaveSocialRoom(room.id)
      navigate('/social', { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível sair da sala.'))
      setLeaving(false)
    }
  }

  if (loading) {
    return (
      <main className="grid min-h-svh place-items-center bg-canvas text-white">
        <div className="size-12 animate-spin rounded-full border-4 border-white/10 border-t-reel" aria-label="Carregando sala" />
      </main>
    )
  }

  if (!room) {
    return (
      <main className="grid min-h-svh place-items-center bg-canvas px-5 text-center text-white">
        <div>
          <p className="font-bold text-red-100">{error || 'Sala não encontrada.'}</p>
          <button type="button" onClick={() => navigate('/social')} className="mt-5 font-semibold text-reel-bright">Voltar</button>
        </div>
      </main>
    )
  }

  const providerOptions = room.commonProviders.map((provider) => ({
    value: provider.id,
    label: provider.name,
  }))
  const waitingForMembers = room.members.length < 2
  const noCommonProviders = room.members.length >= 2 && room.commonProviders.length === 0
  const currentMember = room.members.find((member) => member.userId === user?.id)
  const allReady = room.members.length >= 2 && room.members.every((member) => member.ready)
  const genreLabels = new Map(GENRE_OPTIONS.map((genre) => [genre.value, genre.label]))

  return (
    <main className="min-h-svh bg-canvas px-4 pt-5 pb-28 text-paper sm:px-8 lg:pb-12">
      <div className="pointer-events-none fixed inset-x-0 top-0 h-[34rem] bg-[radial-gradient(circle_at_50%_12%,rgba(233,54,69,.12),transparent_38%)]" />
      <AppHeader
        accessory={
          <button
            type="button"
            onClick={() => void handleLeave()}
            disabled={leaving}
            className="rounded-xl border border-white/12 px-3 py-2 text-xs font-medium text-white/65 transition hover:border-white/25 hover:text-white"
          >
            {leaving ? 'Saindo…' : room.currentUserHost ? 'Encerrar sala' : 'Sair da sala'}
          </button>
        }
      />

      <div className="relative mx-auto max-w-3xl pt-7">
        <section className="rounded-2xl border border-reel/20 bg-reel/[0.045] p-5">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="reelz-kicker">
                {room.type === 'COUPLE' ? 'Modo casal' : 'Modo grupo'}
              </p>
              <h1 className="mt-1 text-2xl font-bold">Sala de {room.hostDisplayName}</h1>
              <p className="mt-1 text-xs text-white/55">{room.members.length}/{room.capacity} participantes</p>
            </div>
            <button
              type="button"
              onClick={() => void handleCopyInvite()}
              className="rounded-lg bg-paper px-3 py-2 text-xs font-bold text-canvas"
            >
              Convidar
            </button>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            {room.members.map((member) => (
              <span key={member.userId} className={`rounded-lg border px-3 py-1.5 text-xs font-semibold ${member.ready ? 'border-emerald-300/25 bg-emerald-300/[0.08] text-emerald-100' : 'border-white/10 bg-black/15 text-white/60'}`}>
                {member.ready ? '✓ ' : ''}{member.host ? 'Anfitrião · ' : ''}{member.displayName}
              </span>
            ))}
          </div>
          <p className="mt-4 text-xs text-white/55">Convite: <strong className="tracking-[.14em] text-white/80">{room.inviteCode}</strong></p>
        </section>

        {error ? <p role="alert" className="mt-4 rounded-2xl border border-red-300/15 bg-red-300/[0.06] p-4 text-sm font-bold text-red-100">{error}</p> : null}

        <section className="mt-4 rounded-2xl border border-white/10 bg-white/[0.025] p-4 sm:p-5" aria-label="Palpites dos participantes">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-sm font-semibold">Palpites da sala</h2>
            <span className="text-[10px] font-semibold text-white/55">{room.members.filter((member) => member.ready).length}/{room.members.length} prontos</span>
          </div>
          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            {room.members.map((member) => {
              const choices = [
                ...member.selectedGenreIds.map((genreId) => genreLabels.get(genreId) ?? `Gênero ${genreId}`),
                ...(member.selectedVibeName ? [member.selectedVibeName] : []),
              ]
              return (
                <div key={member.userId} className="rounded-xl border border-white/10 bg-black/10 p-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-semibold text-white/80">{member.displayName}</span>
                    <span className={`text-[10px] font-semibold ${member.ready ? 'text-emerald-300' : 'text-gold/80'}`}>
                      {member.ready ? 'PRONTO' : 'ESCOLHENDO'}
                    </span>
                  </div>
                  <p className="mt-2 text-xs leading-5 text-white/55">
                    {choices.length > 0 ? choices.join(' · ') : 'Ainda não deu seu pitaco.'}
                  </p>
                </div>
              )
            })}
          </div>
        </section>

        <section className="flex min-h-[25rem] items-center justify-center py-8 text-center">
          <AnimatePresence mode="wait">
            {spinning ? <SlotMachine key="social-spinning" /> : null}
            {!spinning && movie ? (
              <motion.article
                key={`${movie.id}-${room.lastSpinNumber}`}
                initial={{ opacity: 0, scale: 0.5, y: 50 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.8 }}
                transition={{ type: 'spring', bounce: 0.48, duration: 0.85 }}
                className="grid w-full max-w-xl grid-cols-[8.5rem_1fr] gap-4 rounded-2xl border border-white/10 bg-surface p-4 text-left shadow-2xl sm:grid-cols-[11rem_1fr] sm:p-6"
              >
                <div className="aspect-[2/3] overflow-hidden rounded-2xl bg-white/5">
                  {movie.posterPath ? <img src={resolveCatalogImageUrl(movie.posterPath) ?? undefined} alt={`Pôster de ${movie.title}`} className="h-full w-full object-cover" /> : null}
                </div>
                <div className="flex min-w-0 flex-col">
                  <p className="text-xs font-semibold text-gold">{movie.tmdbRating ? `★ ${Number(movie.tmdbRating).toFixed(1)}` : 'Escolha do grupo'}</p>
                  <h2 className="mt-2 text-2xl font-bold leading-tight">{movie.title}</h2>
                  <p className="mt-3 line-clamp-5 text-xs leading-5 text-white/60">{movie.overview || 'Sinopse indisponível.'}</p>
                  {movie.streamingAvailability[0]?.attributionUrl ? (
                    <a
                      href={movie.streamingAvailability[0].attributionUrl ?? undefined}
                      target="_blank"
                      rel="noreferrer"
                      onClick={() => {
                        void trackProductEvent('WATCH_PROVIDER_CLICKED', {
                          movieId: movie.tmdbId,
                          providerId: movie.streamingAvailability[0].providerId,
                        }).then(refreshAchievements).catch(() => {
                          // O clique segue para o streaming mesmo se a telemetria falhar.
                        })
                      }}
                      className="mt-auto rounded-lg bg-reel px-3 py-2.5 text-center text-xs font-bold text-white"
                    >
                      Assistir na {movie.streamingAvailability[0].providerName}
                    </a>
                  ) : movie.streamingAvailability[0] ? (
                    <p className="mt-auto pt-4 text-xs font-bold text-white/45">
                      Disponível na {movie.streamingAvailability[0].providerName}
                    </p>
                  ) : null}
                  <button type="button" onClick={() => void handleWatched()} className="pt-3 text-left text-xs font-semibold text-reel-bright">
                    Já vi · adicionar à coleção
                  </button>
                </div>
              </motion.article>
            ) : null}
            {!spinning && !movie ? (
              <motion.div key="social-idle" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="max-w-md">
                <svg viewBox="0 0 24 24" className="mx-auto size-12 text-reel-bright" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="m10 8.8 5 3.2-5 3.2V8.8Z" /></svg>
                <h2 className="mt-5 text-3xl font-bold tracking-[-.035em]">
                  {waitingForMembers ? 'Esperando companhia' : noCommonProviders ? 'Falta um streaming em comum' : 'Todo mundo pronto?'}
                </h2>
                <p className="mt-3 text-sm leading-6 text-white/60">
                  {waitingForMembers
                    ? 'Compartilhe o convite. A sala começa com pelo menos duas pessoas.'
                    : noCommonProviders
                      ? 'Cada participante deve salvar seus streamings na tela da roleta.'
                      : allReady
                        ? room.currentUserHost ? 'Os palpites combinaram. Você já pode girar.' : 'Todo mundo confirmou. O anfitrião já pode girar.'
                        : 'Cada pessoa escolhe seus gêneros ou vibe e confirma quando estiver pronta.'}
                </p>
              </motion.div>
            ) : null}
          </AnimatePresence>
        </section>

        {currentMember && room.status === 'OPEN' ? (
          <section className="rounded-2xl border border-white/10 bg-white/[0.025] p-4 sm:p-6">
            <p className="reelz-kicker">Seu pitaco</p>
            <h2 className="mt-1 text-xl font-bold">
              {currentMember.ready ? 'Você está pronto' : 'O que você topa assistir?'}
            </h2>
            {currentMember.ready ? (
              <div className="mt-4 flex items-center justify-between gap-3 rounded-2xl border border-emerald-300/15 bg-emerald-300/[0.06] p-4">
                <p className="text-sm font-bold text-emerald-100">✓ Palpite confirmado</p>
                <button
                  type="button"
                  onClick={() => void savePreference(false)}
                  disabled={savingPreference || spinning}
                  className="text-xs font-semibold text-white/65 underline underline-offset-4 disabled:opacity-40"
                >
                  Alterar
                </button>
              </div>
            ) : (
              <>
                <div className="mt-5">
                  <FilterPills
                    legend={`Gêneros · escolha até 3 (${selectedGenres.length}/3)`}
                    options={GENRE_OPTIONS}
                    selectedValues={selectedGenres}
                    onToggle={toggleGenre}
                    disabled={savingPreference || spinning}
                  />
                </div>
                <div className="mt-5">
                  <FilterPills
                    legend="Vibe · opcional"
                    options={vibes}
                    selectedValues={selectedVibe ? [selectedVibe] : []}
                    onToggle={toggleVibe}
                    disabled={savingPreference || spinning}
                  />
                </div>
                <motion.button
                  type="button"
                  whileTap={{ scale: 0.97 }}
                  onClick={() => void savePreference(true)}
                  disabled={savingPreference || spinning || (selectedGenres.length === 0 && selectedVibe === null)}
                  className="mt-6 w-full rounded-xl bg-reel px-5 py-4 text-sm font-bold text-white disabled:bg-white/10 disabled:text-white/40"
                >
                  {savingPreference ? 'Salvando…' : 'Confirmar meu palpite'}
                </motion.button>
              </>
            )}
          </section>
        ) : null}

        {room.currentUserHost && room.status === 'OPEN' ? (
          <section className="mt-4 rounded-2xl border border-white/10 bg-white/[0.025] p-4 sm:p-6">
            <p className="reelz-kicker">Controle do anfitrião</p>
            <div className="mt-5">
              <FilterPills
                legend={quota?.unlimited ? 'Streamings em comum · escolha vários' : 'Streaming em comum · 1 por giro'}
                options={providerOptions}
                selectedValues={selectedProviders}
                onToggle={toggleProvider}
                disabled={spinning}
              />
            </div>
            <motion.button
              type="button"
              whileTap={{ scale: 0.96 }}
              onClick={() => void handleSpin()}
              disabled={spinning || waitingForMembers || noCommonProviders || !allReady || selectedProviders.length === 0}
              className="mt-6 w-full rounded-xl bg-reel px-5 py-4 text-base font-bold shadow-[0_16px_40px_rgba(233,54,69,.2)] disabled:bg-white/10 disabled:text-white/40 disabled:shadow-none"
            >
              {spinning
                ? 'Girando para todos…'
                : !allReady
                  ? `Aguardando ${room.members.filter((member) => !member.ready).length} palpite(s)`
                  : movie ? 'Girar outro filme' : 'Girar para a sala'}
            </motion.button>
          </section>
        ) : null}
      </div>

      <AnimatePresence>
        {toast ? (
          <motion.div initial={{ opacity: 0, y: 25 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} role="status" className="fixed inset-x-4 bottom-24 z-50 mx-auto max-w-md rounded-xl border border-white/12 bg-surface-raised/95 px-4 py-3 text-sm font-semibold text-paper shadow-2xl lg:bottom-6">
            {toast}
          </motion.div>
        ) : null}
      </AnimatePresence>
      <BottomNavigation />
    </main>
  )
}
