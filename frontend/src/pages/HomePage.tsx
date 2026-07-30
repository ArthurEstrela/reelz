import { ReelzLogo } from '../components/brand/ReelzLogo'
import { useAuth } from '../hooks/useAuth'

export function HomePage() {
  const { user, logout } = useAuth()

  return (
    <main className="relative min-h-svh overflow-hidden bg-canvas px-5 py-6 text-white sm:px-8">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_36%,rgba(255,60,72,.14),transparent_26%)]" />

      <header className="relative mx-auto flex max-w-6xl items-center justify-between">
        <ReelzLogo />
        <div className="flex items-center gap-3">
          <span className="hidden max-w-52 truncate text-xs text-white/35 sm:block">{user?.email}</span>
          <button
            type="button"
            onClick={logout}
            className="rounded-lg border border-white/10 px-3 py-2 text-xs font-bold text-white/60 transition hover:border-white/20 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-reel"
          >
            Sair
          </button>
        </div>
      </header>

      <section className="relative mx-auto flex min-h-[calc(100svh-92px)] max-w-3xl flex-col items-center justify-center pb-16 text-center">
        <div className="relative mb-10 grid size-44 place-items-center sm:size-52" aria-hidden="true">
          <div className="absolute inset-0 animate-[spin_24s_linear_infinite] rounded-full border border-dashed border-white/15" />
          <div className="absolute inset-5 rounded-full border border-white/8 bg-white/[0.025] shadow-[0_30px_100px_rgba(255,60,72,.12)]" />
          <div className="absolute inset-10 rounded-full bg-gradient-to-br from-reel to-red-800 shadow-[inset_0_1px_0_rgba(255,255,255,.25)]" />
          <svg viewBox="0 0 24 24" className="relative ml-1 size-10 text-white" aria-hidden="true">
            <path fill="currentColor" d="M8 6.7c0-1.2 1.3-1.9 2.3-1.3l7 4.1a1.7 1.7 0 0 1 0 2.9l-7 4.2A1.5 1.5 0 0 1 8 15.3V6.7Z" />
          </svg>
        </div>

        <p className="mb-4 text-xs font-bold uppercase tracking-[0.25em] text-reel">Em produção</p>
        <h1 className="text-4xl font-black tracking-[-0.055em] sm:text-6xl">Roleta em breve</h1>
        <p className="mt-5 max-w-md text-sm leading-6 text-white/45 sm:text-base sm:leading-7">
          A cabine de projeção está quase pronta. Em breve, um giro vai separar você da próxima grande história.
        </p>

        <div className="mt-10 flex items-center gap-3 rounded-full border border-white/8 bg-white/[0.035] px-4 py-2 text-xs text-white/35">
          <span className="size-1.5 animate-pulse rounded-full bg-emerald-400" />
          Sua sessão está autenticada
        </div>
      </section>
    </main>
  )
}
