import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { AuthLayout } from '../components/auth/AuthLayout'
import { FormMessage } from '../components/feedback/FormMessage'
import { PasswordField } from '../components/form/PasswordField'
import { SubmitButton } from '../components/form/SubmitButton'
import { confirmPasswordReset } from '../services/authService'
import { getApiErrorMessage } from '../utils/apiError'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const token = params.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (password !== confirmation) {
      setError('As senhas precisam ser iguais.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      await confirmPasswordReset(token, password)
      navigate('/login', { replace: true, state: { passwordReset: true } })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível redefinir a senha.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout eyebrow="Nova senha" title="Crie uma chave nova." description="O link funciona uma única vez e expira rapidamente." footer={<Link className="font-bold text-white" to="/forgot-password">Pedir outro link</Link>}>
      {!token ? <FormMessage>Este link não possui um token válido.</FormMessage> : (
        <form onSubmit={submit} className="space-y-5">
          {error && <FormMessage>{error}</FormMessage>}
          <PasswordField id="new-password" label="Nova senha" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} minLength={8} maxLength={128} required autoFocus />
          <PasswordField id="new-password-confirmation" label="Repita a nova senha" autoComplete="new-password" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} minLength={8} maxLength={128} required />
          <SubmitButton loading={loading} loadingLabel="Salvando...">Salvar nova senha</SubmitButton>
        </form>
      )}
    </AuthLayout>
  )
}
