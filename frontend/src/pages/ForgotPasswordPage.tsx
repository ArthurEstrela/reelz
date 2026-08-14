import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { AuthLayout } from '../components/auth/AuthLayout'
import { FormMessage } from '../components/feedback/FormMessage'
import { FormField } from '../components/form/FormField'
import { SubmitButton } from '../components/form/SubmitButton'
import { requestPasswordReset } from '../services/authService'
import { getApiErrorMessage } from '../utils/apiError'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await requestPasswordReset(email)
      setSent(true)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível enviar o link agora.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout eyebrow="Recuperação" title="Volte para a sessão." description="Enviaremos um link temporário para criar uma nova senha." footer={<Link className="font-bold text-white" to="/login">Voltar ao login</Link>}>
      <form onSubmit={submit} className="space-y-5">
        {sent && <FormMessage tone="success">Se esse e-mail estiver cadastrado, o link chegará em instantes.</FormMessage>}
        {error && <FormMessage>{error}</FormMessage>}
        <FormField id="reset-email" label="E-mail" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoFocus />
        <SubmitButton loading={loading} loadingLabel="Enviando...">Enviar link seguro</SubmitButton>
      </form>
    </AuthLayout>
  )
}
