import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { AppHeader } from '../components/navigation/AppHeader'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { FormMessage } from '../components/feedback/FormMessage'
import { FormField } from '../components/form/FormField'
import { PasswordField } from '../components/form/PasswordField'
import { SubmitButton } from '../components/form/SubmitButton'
import { useAuth } from '../hooks/useAuth'
import { deleteCurrentUser, getCurrentUser, updateCurrentUser } from '../services/authService'
import type { UserResponse } from '../types/api'
import { getApiErrorMessage } from '../utils/apiError'

export function AccountPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [profile, setProfile] = useState<UserResponse | null>(null)
  const [displayName, setDisplayName] = useState('')
  const [timezone, setTimezone] = useState('')
  const [countryCode, setCountryCode] = useState('BR')
  const [password, setPassword] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [message, setMessage] = useState<{ text: string; tone: 'error' | 'success' } | null>(null)

  useEffect(() => {
    void getCurrentUser().then((user) => {
      setProfile(user)
      setDisplayName(user.displayName)
      setTimezone(user.timezone)
      setCountryCode(user.countryCode)
    }).catch((error) => setMessage({ text: getApiErrorMessage(error, 'Não foi possível carregar sua conta.'), tone: 'error' }))
  }, [])

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setMessage(null)
    try {
      const updated = await updateCurrentUser({ displayName, timezone, countryCode })
      setProfile(updated)
      setMessage({ text: 'Perfil atualizado.', tone: 'success' })
    } catch (error) {
      setMessage({ text: getApiErrorMessage(error, 'Não foi possível salvar.'), tone: 'error' })
    } finally {
      setSaving(false)
    }
  }

  async function removeAccount() {
    if (!password || !window.confirm('Excluir sua conta e anonimizar seus dados? Esta ação não pode ser desfeita.')) return
    setDeleting(true)
    setMessage(null)
    try {
      await deleteCurrentUser(password)
      logout()
      navigate('/login', { replace: true, state: { accountDeleted: true } })
    } catch (error) {
      setMessage({ text: getApiErrorMessage(error, 'Não foi possível excluir a conta.'), tone: 'error' })
      setDeleting(false)
    }
  }

  return (
    <main className="min-h-svh bg-canvas px-5 pb-28 pt-5 text-white sm:px-8 lg:pb-12 lg:pt-7">
      <AppHeader />
      <div className="mx-auto mt-12 grid w-full max-w-4xl gap-6 lg:grid-cols-[1fr_.8fr]">
        <section className="rounded-3xl border border-white/10 bg-surface p-6 sm:p-8">
          <p className="reelz-kicker">Sua conta</p>
          <h1 className="mt-3 text-3xl font-extrabold tracking-tight text-paper">Perfil e preferências</h1>
          <p className="mt-2 text-sm text-white/50">{profile?.email ?? 'Carregando...'}</p>
          <form onSubmit={save} className="mt-8 space-y-5">
            {message && <FormMessage tone={message.tone}>{message.text}</FormMessage>}
            <FormField id="account-name" label="Nome" value={displayName} onChange={(event) => setDisplayName(event.target.value)} minLength={2} maxLength={80} required />
            <FormField id="account-timezone" label="Fuso horário" value={timezone} onChange={(event) => setTimezone(event.target.value)} maxLength={50} required />
            <FormField id="account-country" label="País (código de 2 letras)" value={countryCode} onChange={(event) => setCountryCode(event.target.value.toUpperCase())} minLength={2} maxLength={2} required />
            <SubmitButton loading={saving} loadingLabel="Salvando...">Salvar perfil</SubmitButton>
          </form>
        </section>
        <div className="space-y-6">
          <section className="rounded-3xl border border-reel/25 bg-reel/[0.07] p-6 sm:p-8">
            <p className="text-xs font-bold uppercase tracking-widest text-reel-bright/80">Seu plano</p>
            <h2 className="mt-3 text-xl font-bold text-paper">{profile?.plan === 'PREMIUM' ? 'Reelz Premium' : 'Reelz Free'}</h2>
            <p className="mt-2 text-sm leading-6 text-white/50">
              {profile?.plan === 'PREMIUM' ? 'Giros ilimitados e todos os streamings combinados.' : 'Conheça os giros ilimitados e a experiência sem anúncios.'}
            </p>
            <Link to="/premium" className="mt-5 inline-flex rounded-xl bg-paper px-4 py-3 text-sm font-bold text-canvas transition hover:bg-white">
              {profile?.plan === 'PREMIUM' ? 'Gerenciar assinatura' : 'Conhecer Premium'}
            </Link>
          </section>
          <section className="rounded-3xl border border-red-400/15 bg-red-400/[0.035] p-6 sm:p-8">
            <p className="text-xs font-bold uppercase tracking-widest text-red-300/70">Zona de cuidado</p>
            <h2 className="mt-3 text-xl font-bold text-paper">Excluir minha conta</h2>
            <p className="mt-2 text-sm leading-6 text-white/50">Seus dados pessoais serão anonimizados e o acesso será encerrado imediatamente.</p>
            <div className="mt-6">
              <PasswordField id="delete-password" label="Confirme sua senha" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} />
              <button type="button" disabled={deleting || !password} onClick={() => void removeAccount()} className="mt-4 w-full rounded-xl border border-red-400/25 px-4 py-3 text-sm font-bold text-red-200 transition hover:bg-red-400/10 disabled:opacity-40">
                {deleting ? 'Excluindo...' : 'Excluir conta permanentemente'}
              </button>
            </div>
          </section>
        </div>
      </div>
      <BottomNavigation />
    </main>
  )
}
