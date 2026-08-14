import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import { AuthLayout } from '../components/auth/AuthLayout'
import { FormMessage } from '../components/feedback/FormMessage'
import { FormField } from '../components/form/FormField'
import { SubmitButton } from '../components/form/SubmitButton'
import { confirmEmailVerification, requestEmailVerification } from '../services/authService'
import { getApiErrorMessage } from '../utils/apiError'

export function VerifyEmailPage() {
  const [params] = useSearchParams()
  const location = useLocation()
  const initialEmail = (location.state as { email?: string } | null)?.email ?? ''
  const token = params.get('token')
  const [email, setEmail] = useState(initialEmail)
  const [state, setState] = useState<'idle' | 'loading' | 'verified' | 'sent' | 'error'>(token ? 'loading' : 'idle')
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    void confirmEmailVerification(token)
      .then(() => setState('verified'))
      .catch((error) => {
        setMessage(getApiErrorMessage(error, 'Não foi possível confirmar este e-mail.'))
        setState('error')
      })
  }, [token])

  async function resend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setState('loading')
    setMessage(null)
    try {
      await requestEmailVerification(email)
      setState('sent')
    } catch (error) {
      setMessage(getApiErrorMessage(error, 'Não foi possível reenviar agora.'))
      setState('error')
    }
  }

  return (
    <AuthLayout eyebrow="Confirmação" title={state === 'verified' ? 'E-mail confirmado.' : 'Confira sua caixa de entrada.'} description="Essa etapa protege sua conta e leva menos de um minuto." footer={<Link className="font-bold text-white" to="/login">Ir para o login</Link>}>
      {state === 'verified' && <FormMessage tone="success">Tudo certo. Sua conta esta pronta para entrar.</FormMessage>}
      {state === 'loading' && <FormMessage tone="success">Validando seu link seguro...</FormMessage>}
      {state === 'sent' && <FormMessage tone="success">Se a conta estiver pendente, enviamos um novo link.</FormMessage>}
      {message && <FormMessage>{message}</FormMessage>}
      {state !== 'verified' && !token && (
        <form onSubmit={resend} className="mt-5 space-y-5">
          <FormField id="verification-email" label="E-mail" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoFocus />
          <SubmitButton loading={state === 'loading'} loadingLabel="Enviando...">Reenviar confirmação</SubmitButton>
        </form>
      )}
      {state === 'error' && token && <Link to="/verify-email" className="mt-5 inline-flex text-sm font-bold text-reel-bright">Solicitar um novo link</Link>}
    </AuthLayout>
  )
}
