import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { ReelzLogo } from '../components/brand/ReelzLogo'
import { BottomNavigation } from '../components/navigation/BottomNavigation'
import { getAnalyticsOverview } from '../services/analyticsService'
import type { AnalyticsOverview } from '../types/analytics'
import { getApiErrorMessage } from '../utils/apiError'

const PERIODS = [7, 30, 90] as const

interface MetricCardProps {
  label: string
  value: string | number
  detail: string
}

function MetricCard({ label, value, detail }: MetricCardProps) {
  return (
    <article className="rounded-2xl border border-white/8 bg-white/[0.035] p-4">
      <p className="text-[10px] font-black tracking-[.14em] text-white/35 uppercase">{label}</p>
      <p className="mt-2 text-3xl font-black tracking-tight text-white">{value}</p>
      <p className="mt-1 text-xs leading-5 text-white/40">{detail}</p>
    </article>
  )
}

function percentage(value: number) {
  return `${value.toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`
}

export function AdminAnalyticsPage() {
  const [days, setDays] = useState<(typeof PERIODS)[number]>(30)
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false

    getAnalyticsOverview(days)
      .then((response) => {
        if (!cancelled) setOverview(response)
      })
      .catch((requestError) => {
        if (!cancelled) {
          setError(getApiErrorMessage(requestError, 'Não foi possível carregar os indicadores.'))
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [days, reloadKey])

  const changePeriod = (period: (typeof PERIODS)[number]) => {
    if (period === days) return
    setLoading(true)
    setError(null)
    setDays(period)
  }

  const reload = () => {
    setLoading(true)
    setError(null)
    setReloadKey((current) => current + 1)
  }

  const chartMaximum = useMemo(() => {
    if (!overview) return 1
    return Math.max(
      1,
      ...overview.daily.flatMap((item) => [item.registrations, item.successfulSpins, item.decisions]),
    )
  }, [overview])

  return (
    <main className="min-h-svh bg-canvas px-4 pt-5 pb-28 text-white sm:px-8">
      <header className="mx-auto flex max-w-6xl items-center justify-between gap-4">
        <ReelzLogo />
        <Link
          to="/"
          className="rounded-xl border border-white/10 px-3 py-2 text-xs font-black text-white/55 transition hover:text-white"
        >
          Voltar à roleta
        </Link>
      </header>

      <div className="mx-auto mt-9 max-w-6xl">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <p className="text-xs font-black tracking-[.18em] text-reel uppercase">Beta cockpit</p>
            <h1 className="mt-2 text-3xl font-black tracking-tight sm:text-4xl">Saúde do produto</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-white/45">
              Métricas agregadas e pseudonimizadas. Nenhum e-mail aparece neste painel.
            </p>
          </div>
          <div className="flex rounded-xl border border-white/8 bg-white/[0.025] p-1">
            {PERIODS.map((period) => (
              <button
                key={period}
                type="button"
                onClick={() => changePeriod(period)}
                className={`rounded-lg px-4 py-2 text-xs font-black transition ${
                  days === period ? 'bg-white text-black' : 'text-white/40 hover:text-white'
                }`}
              >
                {period} dias
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="mt-8 grid grid-cols-2 gap-3 lg:grid-cols-4">
            {Array.from({ length: 8 }, (_, index) => (
              <div key={index} className="h-32 animate-pulse rounded-2xl bg-white/5" />
            ))}
          </div>
        ) : null}

        {error ? (
          <div role="alert" className="mt-8 rounded-2xl border border-red-300/15 bg-red-300/[0.05] p-5">
            <p className="text-sm font-bold text-red-100">{error}</p>
            <button
              type="button"
              onClick={reload}
              className="mt-3 text-xs font-black text-white underline underline-offset-4"
            >
              Tentar novamente
            </button>
          </div>
        ) : null}

        {!loading && overview ? (
          <>
            <section className="mt-8 grid grid-cols-2 gap-3 lg:grid-cols-4" aria-label="Indicadores principais">
              <MetricCard label="Cadastros" value={overview.newUsers} detail={`${overview.totalUsers} usuários no total`} />
              <MetricCard label="Ativação" value={percentage(overview.activationRate)} detail={`${overview.onboardingCompletedUsers} concluíram o onboarding`} />
              <MetricCard label="Primeiro giro" value={overview.firstSpinUsers} detail="Cadastrados do período que já giraram" />
              <MetricCard label="Usuários ativos" value={overview.activeUsers} detail="Com giro ou evento no período" />
              <MetricCard label="Decisão" value={percentage(overview.decisionRate)} detail={`${overview.decidedSessions} de ${overview.homeSessions} sessões`} />
              <MetricCard label="Giros por decisão" value={overview.averageSpinsPerDecision} detail="Menor é melhor; alvo inicial ≤ 3" />
              <MetricCard label="Retenção D7" value={percentage(overview.d7RetentionRate)} detail={`${overview.d7RetainedUsers} de ${overview.d7EligibleUsers} elegíveis`} />
              <MetricCard label="Aberturas do streaming" value={overview.providerClicks} detail={`${overview.successfulSpins} giros com sucesso`} />
            </section>

            <section className="mt-6 grid gap-4 lg:grid-cols-[1.35fr_.65fr]">
              <article className="rounded-[1.75rem] border border-white/8 bg-white/[0.025] p-5">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <h2 className="font-black">Movimento diário</h2>
                    <p className="mt-1 text-xs text-white/35">Cadastro, giro e decisão em UTC.</p>
                  </div>
                  <div className="flex gap-3 text-[10px] font-bold text-white/45">
                    <span className="text-sky-300">● cadastro</span>
                    <span className="text-reel">● giro</span>
                    <span className="text-emerald-300">● decisão</span>
                  </div>
                </div>
                <div className="mt-6 flex h-48 items-end gap-1 overflow-hidden" aria-label="Gráfico diário">
                  {overview.daily.map((item) => (
                    <div key={item.date} className="group flex min-w-2 flex-1 items-end gap-px" title={`${item.date}: ${item.successfulSpins} giros`}>
                      <div className="w-1/3 rounded-t bg-sky-300/70" style={{ height: `${Math.max(2, item.registrations / chartMaximum * 100)}%` }} />
                      <div className="w-1/3 rounded-t bg-reel/80" style={{ height: `${Math.max(2, item.successfulSpins / chartMaximum * 100)}%` }} />
                      <div className="w-1/3 rounded-t bg-emerald-300/70" style={{ height: `${Math.max(2, item.decisions / chartMaximum * 100)}%` }} />
                    </div>
                  ))}
                </div>
              </article>

              <article className="rounded-[1.75rem] border border-violet-300/10 bg-violet-300/[0.035] p-5">
                <p className="text-[10px] font-black tracking-[.16em] text-violet-200/55 uppercase">Uso social</p>
                <h2 className="mt-2 text-xl font-black">Casal e grupo</h2>
                <div className="mt-5 grid grid-cols-2 gap-3">
                  <div className="rounded-2xl bg-pink-300/[0.07] p-4">
                    <span className="text-2xl font-black text-pink-100">{overview.socialRoomsCreated}</span>
                    <p className="text-xs text-white/40">salas criadas</p>
                  </div>
                  <div className="rounded-2xl bg-violet-300/[0.07] p-4">
                    <span className="text-2xl font-black text-violet-100">{overview.socialRoomsWithSpin}</span>
                    <p className="text-xs text-white/40">salas que giraram</p>
                  </div>
                  <div className="rounded-2xl bg-white/[0.04] p-4">
                    <span className="text-2xl font-black text-white">{overview.socialSpins}</span>
                    <p className="text-xs text-white/40">giros sociais</p>
                  </div>
                  <div className="rounded-2xl bg-white/[0.04] p-4">
                    <span className="text-2xl font-black text-white">{overview.socialParticipants}</span>
                    <p className="text-xs text-white/40">participantes únicos</p>
                  </div>
                </div>
                <p className="mt-5 text-xs leading-5 text-white/35">
                  Interesse pré-lançamento: {overview.coupleModeInterestedUsers} casal · {overview.groupModeInterestedUsers} grupo.
                </p>
              </article>
            </section>

            <section className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
              <MetricCard label="Quero ver" value={overview.watchlistedMovies} detail="Filmes salvos no período" />
              <MetricCard label="Já assisti" value={overview.watchedMovies} detail="Histórico atualizado no período" />
              <MetricCard label="Sessões" value={overview.homeSessions} detail="Visitas autenticadas à home" />
              <MetricCard label="Decisões" value={overview.decidedSessions} detail="Sessões que abriram onde assistir" />
            </section>

            <section className="mt-6 rounded-[1.75rem] border border-white/8 bg-white/[0.025] p-5">
              <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
                <div>
                  <p className="text-[10px] font-black tracking-[.16em] text-amber-200/55 uppercase">Voz dos testadores</p>
                  <h2 className="mt-1 text-xl font-black">Feedback do beta</h2>
                  <p className="mt-1 text-xs text-white/35">Sem e-mail ou identificação individual no painel.</p>
                </div>
                <div className="flex gap-3">
                  <div className="rounded-xl bg-white/5 px-4 py-2 text-center">
                    <p className="text-xl font-black">{overview.averageFeedbackScore || '—'}</p>
                    <p className="text-[10px] text-white/35">média / 5</p>
                  </div>
                  <div className="rounded-xl bg-white/5 px-4 py-2 text-center">
                    <p className="text-xl font-black">{overview.feedbackCount}</p>
                    <p className="text-[10px] text-white/35">respostas</p>
                  </div>
                </div>
              </div>

              {overview.recentFeedback.length ? (
                <div className="mt-5 grid gap-3 md:grid-cols-2">
                  {overview.recentFeedback.map((feedback, index) => (
                    <article key={`${feedback.createdAt}-${index}`} className="rounded-2xl border border-white/6 bg-black/15 p-4">
                      <div className="flex items-center justify-between gap-3">
                        <span className="font-black text-amber-200">{'★'.repeat(feedback.score)}<span className="text-white/10">{'★'.repeat(5 - feedback.score)}</span></span>
                        <time className="text-[10px] text-white/25" dateTime={feedback.createdAt}>
                          {new Date(feedback.createdAt).toLocaleDateString('pt-BR')}
                        </time>
                      </div>
                      <p className="mt-2 text-sm leading-6 text-white/55">
                        {feedback.message || 'A pessoa enviou apenas a nota.'}
                      </p>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="mt-5 rounded-2xl bg-white/[0.025] p-4 text-sm text-white/35">
                  Nenhum feedback neste período ainda.
                </p>
              )}
            </section>
          </>
        ) : null}
      </div>
      <BottomNavigation />
    </main>
  )
}
