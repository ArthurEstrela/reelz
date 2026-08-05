import axios from 'axios'
import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { AuthLayout } from '../components/auth/AuthLayout'
import { FormMessage } from '../components/feedback/FormMessage'
import { FormField } from '../components/form/FormField'
import { PasswordField } from '../components/form/PasswordField'
import { SubmitButton } from '../components/form/SubmitButton'
import { useAuth } from '../hooks/useAuth'
import type { ApiErrorResponse } from '../types/api'
import { getApiErrorMessage } from '../utils/apiError'

interface LoginNavigationState {
  from?: { pathname?: string }
  registered?: boolean
}

function getSafeReturnPath(pathname?: string): string {
  if (!pathname || !pathname.startsWith('/') || pathname.startsWith('//') || pathname.includes('\\')) {
    return '/'
  }
  return pathname
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const navigationState = location.state as LoginNavigationState | null
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setErrorMessage(null)

    try {
      await login(email, password)
      navigate(getSafeReturnPath(navigationState?.from?.pathname), { replace: true })
    } catch (error) {
      const isCredentialsError =
        axios.isAxiosError<ApiErrorResponse>(error) &&
        (error.response?.status === 401 || error.response?.status === 403)
      setErrorMessage(
        isCredentialsError
          ? error.response?.data?.message || 'E-mail ou senha inválidos.'
          : getApiErrorMessage(error, 'Não foi possível entrar. Tente novamente.'),
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Bem-vindo de volta"
      title="Bom te ver de novo."
      description="Entre para continuar de onde parou."
      footer={
        <>
          Ainda não tem uma conta?{' '}
          <Link
            className="font-bold text-white transition hover:text-reel"
            to="/register"
            state={{ from: navigationState?.from }}
          >
            Criar agora
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5" noValidate={false}>
        {navigationState?.registered && (
          <FormMessage tone="success">Conta criada! Agora é só entrar.</FormMessage>
        )}
        {errorMessage && <FormMessage>{errorMessage}</FormMessage>}

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
          autoFocus
        />

        <PasswordField
          id="password"
          label="Senha"
          autoComplete="current-password"
          placeholder="Sua senha"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          maxLength={128}
          required
        />

        <SubmitButton loading={loading} loadingLabel="Entrando...">
          Entrar no Reelz
        </SubmitButton>
      </form>
    </AuthLayout>
  )
}
