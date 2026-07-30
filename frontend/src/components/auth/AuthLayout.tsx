import type { PropsWithChildren, ReactNode } from 'react'
import { ReelzLogo } from '../brand/ReelzLogo'

interface AuthLayoutProps extends PropsWithChildren {
  eyebrow: string
  title: string
  description: string
  footer: ReactNode
}

export function AuthLayout({
  eyebrow,
  title,
  description,
  footer,
  children,
}: AuthLayoutProps) {
  return (
    <main className="relative min-h-svh overflow-hidden bg-canvas text-white">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_15%_15%,rgba(255,62,72,0.16),transparent_28%),radial-gradient(circle_at_90%_85%,rgba(126,68,255,0.11),transparent_30%)]" />
      <div className="pointer-events-none absolute inset-0 opacity-[0.035] [background-image:linear-gradient(rgba(255,255,255,.8)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,.8)_1px,transparent_1px)] [background-size:48px_48px]" />

      <div className="relative mx-auto grid min-h-svh max-w-7xl lg:grid-cols-[1.05fr_.95fr]">
        <section className="hidden min-h-svh flex-col justify-between border-r border-white/8 p-12 lg:flex xl:p-16">
          <ReelzLogo />

          <div className="max-w-xl pb-8">
            <div className="mb-8 flex items-center gap-3 text-xs font-bold uppercase tracking-[0.24em] text-white/40">
              <span className="h-px w-10 bg-reel" />
              Sua sessão começa agora
            </div>
            <h2 className="max-w-lg text-5xl font-black leading-[1.02] tracking-[-0.055em] text-white xl:text-6xl">
              Menos tempo escolhendo.{' '}
              <span className="text-reel">Mais tempo assistindo.</span>
            </h2>
            <p className="mt-6 max-w-md text-base leading-7 text-white/48">
              Uma roleta inteligente que encontra o filme certo nos streamings que você já tem.
            </p>

            <div className="mt-12 grid max-w-lg grid-cols-3 gap-3" aria-hidden="true">
              {['01', '02', '03'].map((number, index) => (
                <div
                  key={number}
                  className={`aspect-[3/4] rounded-2xl border border-white/10 bg-gradient-to-b p-4 shadow-2xl ${
                    index === 1
                      ? '-translate-y-5 from-reel/25 to-surface'
                      : 'from-white/8 to-surface'
                  }`}
                >
                  <span className="text-[10px] font-bold tracking-[.2em] text-white/30">
                    REEL {number}
                  </span>
                  <div className="mt-auto flex h-full items-end pb-2">
                    <span className="block h-1 w-full rounded-full bg-white/10">
                      <span className="block h-full rounded-full bg-reel" style={{ width: `${35 + index * 25}%` }} />
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <p className="text-xs text-white/25">© 2026 Reelz. Feito para a próxima sessão.</p>
        </section>

        <section className="flex min-h-svh items-center justify-center px-5 py-8 sm:px-10 lg:px-14">
          <div className="w-full max-w-md">
            <div className="mb-10 lg:hidden">
              <ReelzLogo />
            </div>

            <div className="mb-8">
              <p className="mb-3 text-xs font-bold uppercase tracking-[0.22em] text-reel">{eyebrow}</p>
              <h1 className="text-3xl font-black tracking-[-0.045em] text-white sm:text-4xl">{title}</h1>
              <p className="mt-3 max-w-sm text-sm leading-6 text-white/48">{description}</p>
            </div>

            {children}

            <div className="mt-8 text-center text-sm text-white/45">{footer}</div>
          </div>
        </section>
      </div>
    </main>
  )
}
