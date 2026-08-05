import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { joinSocialRoom } from '../services/socialService'
import { getApiErrorMessage } from '../utils/apiError'

export function SocialJoinPage() {
  const { inviteCode = '' } = useParams()
  const navigate = useNavigate()
  const started = useRef(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (started.current) return
    started.current = true
    joinSocialRoom(inviteCode)
      .then((room) => navigate(`/social/rooms/${room.id}`, { replace: true }))
      .catch((requestError) => setError(getApiErrorMessage(requestError, 'Não foi possível aceitar este convite.')))
  }, [inviteCode, navigate])

  return (
    <main className="grid min-h-svh place-items-center bg-canvas px-5 text-center text-paper">
      <div>
        {error ? (
          <>
            <p className="font-bold text-red-100">{error}</p>
            <button type="button" onClick={() => navigate('/social')} className="mt-5 font-bold text-reel-bright underline underline-offset-4">Ir para salas</button>
          </>
        ) : (
          <>
            <div className="mx-auto size-12 animate-spin rounded-full border-4 border-white/10 border-t-reel" />
            <p className="mt-5 text-sm font-semibold text-white/65">Entrando na sala…</p>
          </>
        )}
      </div>
    </main>
  )
}
