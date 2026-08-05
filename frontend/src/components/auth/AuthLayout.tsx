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
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_12%_12%,rgba(233,54,69,0.13),transparent_32%)]" />

      <div className="relative mx-auto grid min-h-svh max-w-7xl lg:grid-cols-[1.05fr_.95fr]">
        <section className="hidden min-h-svh flex-col justify-between border-r border-white/8 p-12 lg:flex xl:p-16">
          <ReelzLogo />

          <div className="max-w-xl pb-8">
            <div className="mb-8 flex items-center gap-3 text-xs font-semibold uppercase tracking-[0.14em] text-white/55">
              <span className="h-px w-10 bg-reel" />
              Sessão sem enrolação
            </div>
            <h2 className="max-w-lg text-5xl font-extrabold leading-[1.02] tracking-[-0.05em] text-paper xl:text-6xl">
              O filme da noite,{' '}
              <span className="text-reel-bright">sem reunião de pauta.</span>
            </h2>
            <p className="mt-6 max-w-md text-base leading-7 text-white/60">
              Você diz onde assiste e o que está a fim. O Reelz cuida do resto.
            </p>

            <div className="mt-12 max-w-lg border-y border-white/12 py-5" aria-hidden="true">
              <div className="grid grid-cols-3 divide-x divide-white/10">
                {[['01', 'Seus streamings'], ['02', 'Seu clima'], ['03', 'Um filme']].map(([number, label]) => (
                  <div key={number} className="px-4 first:pl-0">
                    <span className="text-xs font-semibold text-reel-bright">{number}</span>
                    <span className="mt-2 block text-sm font-medium text-white/70">{label}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <p className="text-xs text-white/50">© 2026 Reelz. Feito para a próxima sessão.</p>
        </section>

        <section className="flex min-h-svh items-center justify-center px-5 py-8 sm:px-10 lg:px-14">
          <div className="w-full max-w-md">
            <div className="mb-10 lg:hidden">
              <ReelzLogo />
            </div>

            <div className="mb-8">
              <p className="reelz-kicker mb-3">{eyebrow}</p>
              <h1 className="text-3xl font-extrabold tracking-[-0.04em] text-paper sm:text-4xl">{title}</h1>
              <p className="mt-3 max-w-sm text-sm leading-6 text-white/60">{description}</p>
            </div>

            {children}

            <div className="mt-8 text-center text-sm text-white/45">{footer}</div>
          </div>
        </section>
      </div>
    </main>
  )
}
