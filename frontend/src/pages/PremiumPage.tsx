import { useCallback, useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { useSearchParams } from 'react-router'
import { AppHeader } from '../components/navigation/AppHeader'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { FormMessage } from '../components/feedback/FormMessage'
import {
  cancelCurrentSubscription,
  createSubscriptionCheckout,
  getBillingPlans,
  getCurrentSubscription,
} from '../services/billingService'
import type { BillingPlan, BillingPlanCode, BillingSubscription } from '../types/billing'
import { getApiErrorMessage } from '../utils/apiError'

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

function planCaption(plan: BillingPlan) {
  if (plan.interval === 'ANNUAL') {
    return `${currencyFormatter.format(plan.priceCents / 1_200)}/mês no plano anual`
  }
  return 'cobrado mensalmente'
}

function statusMessage(subscription: BillingSubscription) {
  if (subscription.premium) return 'Seu Premium está ativo.'
  if (subscription.status === 'CHECKOUT_PENDING') return 'Você tem um pagamento aguardando conclusão.'
  if (subscription.status === 'PAST_DUE') return 'Não conseguimos renovar sua assinatura.'
  if (subscription.status === 'CANCELED') return 'Sua assinatura foi cancelada.'
  return null
}

export function PremiumPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const returningFromCheckout = searchParams.get('checkout') === 'success'
  const [plans, setPlans] = useState<BillingPlan[]>([])
  const [subscription, setSubscription] = useState<BillingSubscription | null>(null)
  const [loading, setLoading] = useState(true)
  const [checkingPayment, setCheckingPayment] = useState(returningFromCheckout)
  const [checkoutPlan, setCheckoutPlan] = useState<BillingPlanCode | null>(null)
  const [canceling, setCanceling] = useState(false)
  const [message, setMessage] = useState<{ tone: 'error' | 'success'; text: string } | null>(null)

  const loadSubscription = useCallback(async () => {
    const current = await getCurrentSubscription()
    setSubscription(current)
    return current
  }, [])

  useEffect(() => {
    let active = true
    void Promise.all([getBillingPlans(), getCurrentSubscription()])
      .then(([nextPlans, current]) => {
        if (!active) return
        setPlans(nextPlans)
        setSubscription(current)
      })
      .catch((error) => {
        if (active) setMessage({ tone: 'error', text: getApiErrorMessage(error, 'Não foi possível carregar os planos.') })
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!returningFromCheckout) return
    let active = true
    let attempts = 0
    const check = async () => {
      attempts += 1
      try {
        const current = await loadSubscription()
        if (!active) return
        if (current.premium) {
          setCheckingPayment(false)
          setMessage({ tone: 'success', text: 'Pagamento confirmado. Bem-vindo ao CineGiro Premium!' })
          setSearchParams({}, { replace: true })
          return
        }
      } catch {
        // The normal page error is enough if every polling attempt fails.
      }
      if (active && attempts < 10) window.setTimeout(() => void check(), 2_000)
      else if (active) {
        setCheckingPayment(false)
        setMessage({
          tone: 'success',
          text: 'Pagamento recebido. A confirmação ainda está chegando; atualize esta tela em instantes.',
        })
      }
    }
    void check()
    return () => { active = false }
  }, [loadSubscription, returningFromCheckout, setSearchParams])

  async function startCheckout(planCode: BillingPlanCode) {
    setCheckoutPlan(planCode)
    setMessage(null)
    try {
      const checkout = await createSubscriptionCheckout(planCode)
      window.location.assign(checkout.checkoutUrl)
    } catch (error) {
      setMessage({ tone: 'error', text: getApiErrorMessage(error, 'Não foi possível abrir o pagamento.') })
      setCheckoutPlan(null)
    }
  }

  async function cancelSubscription() {
    if (!window.confirm('Cancelar o Premium agora? O acesso é encerrado imediatamente e não haverá novas cobranças.')) return
    setCanceling(true)
    setMessage(null)
    try {
      const current = await cancelCurrentSubscription()
      setSubscription(current)
      setMessage({ tone: 'success', text: 'Assinatura cancelada. Não haverá novas cobranças.' })
    } catch (error) {
      setMessage({ tone: 'error', text: getApiErrorMessage(error, 'Não foi possível cancelar a assinatura.') })
    } finally {
      setCanceling(false)
    }
  }

  const accountStatus = useMemo(() => subscription && statusMessage(subscription), [subscription])

  return (
    <main className="min-h-svh overflow-hidden bg-canvas px-5 pb-28 pt-5 text-white sm:px-8 lg:pb-12 lg:pt-7">
      <div className="pointer-events-none fixed inset-x-0 top-0 h-[34rem] bg-[radial-gradient(circle_at_50%_0%,rgba(125,92,255,.2),transparent_65%)]" />
      <AppHeader />

      <div className="relative mx-auto mt-12 w-full max-w-5xl">
        <motion.div initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} className="mx-auto max-w-2xl text-center">
          <p className="cinegiro-kicker">CineGiro Premium</p>
          <h1 className="mt-4 text-4xl font-black tracking-[-0.04em] text-paper sm:text-5xl">
            Menos limite. Mais histórias.
          </h1>
          <p className="mx-auto mt-4 max-w-xl text-sm leading-6 text-white/55 sm:text-base">
            Gire sem contar, combine todos os seus streamings e mantenha a experiência limpa — sem anúncios no caminho.
          </p>
        </motion.div>

        {message ? <div className="mx-auto mt-7 max-w-xl"><FormMessage tone={message.tone}>{message.text}</FormMessage></div> : null}

        {accountStatus ? (
          <section className="mx-auto mt-7 flex max-w-xl flex-col gap-4 rounded-2xl border border-brand/25 bg-brand/[0.08] p-5 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="font-bold text-paper">{accountStatus}</p>
              {subscription?.currentPeriodEnd ? (
                <p className="mt-1 text-xs text-white/50">Período atual até {new Date(subscription.currentPeriodEnd).toLocaleDateString('pt-BR')}</p>
              ) : null}
            </div>
            {subscription?.checkoutUrl ? (
              <button type="button" onClick={() => window.location.assign(subscription.checkoutUrl!)} className="rounded-xl bg-paper px-4 py-3 text-sm font-bold text-canvas">
                Continuar pagamento
              </button>
            ) : null}
          </section>
        ) : null}

        {checkingPayment ? (
          <div className="mx-auto mt-8 flex max-w-md items-center justify-center gap-3 rounded-2xl border border-white/10 bg-surface px-5 py-4 text-sm text-white/65">
            <span className="size-4 animate-spin rounded-full border-2 border-white/20 border-t-brand-bright" />
            Confirmando seu pagamento com segurança…
          </div>
        ) : null}

        <div className="mt-10 grid gap-5 md:grid-cols-2">
          {loading ? [0, 1].map((item) => <div key={item} className="h-96 animate-pulse rounded-3xl border border-white/8 bg-white/[0.035]" />) : plans.map((plan, index) => (
            <motion.article
              key={plan.code}
              initial={{ opacity: 0, y: 24 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.08, type: 'spring', stiffness: 180, damping: 22 }}
              className={`relative rounded-3xl border p-6 sm:p-8 ${plan.recommended ? 'border-brand/45 bg-brand/[0.09] shadow-[0_28px_90px_rgba(91,61,196,.2)]' : 'border-white/10 bg-surface'}`}
            >
              {plan.recommended ? <span className="absolute right-5 top-5 rounded-full bg-brand px-3 py-1 text-[0.65rem] font-black uppercase tracking-wider">Melhor escolha</span> : null}
              <p className="text-sm font-bold text-white/60">{plan.name}</p>
              <div className="mt-5 flex items-end gap-2">
                <span className="text-4xl font-black tracking-tight text-paper">{currencyFormatter.format(plan.priceCents / 100)}</span>
                <span className="pb-1 text-sm text-white/45">/{plan.interval === 'ANNUAL' ? 'ano' : 'mês'}</span>
              </div>
              <p className="mt-2 text-xs text-white/45">{planCaption(plan)}</p>
              <ul className="mt-7 space-y-3">
                {plan.features.map((feature) => (
                  <li key={feature} className="flex items-center gap-3 text-sm text-white/70">
                    <span className="grid size-5 place-items-center rounded-full bg-brand/20 text-xs font-black text-brand-bright">✓</span>
                    {feature}
                  </li>
                ))}
              </ul>
              <button
                type="button"
                disabled={!plan.available || checkoutPlan !== null || Boolean(subscription?.premium)}
                onClick={() => void startCheckout(plan.code)}
                className={`mt-8 w-full rounded-xl px-5 py-3.5 text-sm font-black transition disabled:cursor-not-allowed disabled:opacity-45 ${plan.recommended ? 'bg-paper text-canvas hover:bg-white' : 'bg-brand text-white hover:bg-brand-bright'}`}
              >
                {checkoutPlan === plan.code ? 'Abrindo checkout…' : subscription?.premium ? 'Plano ativo' : plan.available ? 'Assinar Premium' : 'Disponível em breve'}
              </button>
            </motion.article>
          ))}
        </div>

        <p className="mx-auto mt-8 max-w-xl text-center text-xs leading-5 text-white/35">
          Pagamento processado pela AbacatePay. O Premium só é ativado após a confirmação segura do pagamento.
        </p>

        {subscription?.cancelable ? (
          <div className="mt-10 text-center">
            <button type="button" disabled={canceling} onClick={() => void cancelSubscription()} className="text-xs font-semibold text-white/40 underline decoration-white/20 underline-offset-4 transition hover:text-red-200 disabled:opacity-50">
              {canceling ? 'Cancelando…' : 'Cancelar assinatura'}
            </button>
          </div>
        ) : null}
      </div>
      <BottomNavigation />
    </main>
  )
}
