import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { AuthLayout } from '../components/auth/AuthLayout'
import { FormMessage } from '../components/feedback/FormMessage'
import { FormField } from '../components/form/FormField'
import { PasswordField } from '../components/form/PasswordField'
import { SubmitButton } from '../components/form/SubmitButton'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../utils/apiError'
import { getBrowserCountryCode, getBrowserTimezone } from '../utils/locale'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const navigationState = location.state as { from?: { pathname?: string } } | null
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [termsAccepted, setTermsAccepted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setErrorMessage(null)

    try {
      await register({
        displayName,
        email,
        password,
        termsAccepted,
        timezone: getBrowserTimezone(),
        countryCode: getBrowserCountryCode(),
      })
      navigate('/login', {
        replace: true,
        state: { registered: true, from: navigationState?.from },
      })
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(error, 'Não foi possível criar sua conta. Revise os dados e tente novamente.'),
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Sua primeira sessão"
      title="Crie sua conta."
      description="Leva menos de um minuto. Escolher seu próximo filme vai levar ainda menos."
      footer={
        <>
          Já tem uma conta?{' '}
          <Link
            className="font-bold text-white transition hover:text-reel"
            to="/login"
            state={{ from: navigationState?.from }}
          >
            Fazer login
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        {errorMessage && <FormMessage>{errorMessage}</FormMessage>}

        <FormField
          id="displayName"
          label="Como podemos te chamar?"
          type="text"
          autoComplete="name"
          placeholder="Seu nome"
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          minLength={2}
          maxLength={80}
          required
          autoFocus
        />

        <FormField
          id="email"
          label="E-mail"
          type="email"
          inputMode="email"
          autoComplete="email"
          placeholder="voce@exemplo.com"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          maxLength={254}
          required
        />

        <PasswordField
          id="password"
          label="Senha"
          autoComplete="new-password"
          placeholder="Mínimo de 8 caracteres"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          minLength={8}
          maxLength={128}
          required
        />

        <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-white/8 bg-white/[0.025] p-4 transition hover:border-white/15">
          <input
            type="checkbox"
            checked={termsAccepted}
            onChange={(event) => setTermsAccepted(event.target.checked)}
            required
            className="mt-0.5 size-4 shrink-0 accent-reel"
          />
          <span className="text-xs leading-5 text-white/48">
            Li e aceito os <Link to="/terms" target="_blank" className="font-semibold text-white/72 underline decoration-white/20 underline-offset-2">Termos de Uso</Link> e a{' '}
            <Link to="/privacy" target="_blank" className="font-semibold text-white/72 underline decoration-white/20 underline-offset-2">Política de Privacidade</Link>.
          </span>
        </label>

        <SubmitButton loading={loading} loadingLabel="Criando sua conta...">
          Criar conta grátis
        </SubmitButton>
      </form>
    </AuthLayout>
  )
}
