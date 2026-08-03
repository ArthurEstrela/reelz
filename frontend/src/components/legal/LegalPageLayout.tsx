import type { PropsWithChildren } from 'react'
import { Link } from 'react-router'
import { ReelzLogo } from '../brand/ReelzLogo'

interface LegalPageLayoutProps extends PropsWithChildren {
  eyebrow: string
  title: string
  updatedAt: string
}

export function LegalPageLayout({ eyebrow, title, updatedAt, children }: LegalPageLayoutProps) {
  return (
    <main className="min-h-svh bg-canvas px-5 py-6 text-white sm:px-8">
      <header className="mx-auto flex max-w-3xl items-center justify-between gap-4">
        <ReelzLogo />
        <Link to="/" className="text-xs font-black text-white/45 transition hover:text-white">
          Voltar
        </Link>
      </header>
      <article className="prose-invert mx-auto mt-12 max-w-3xl pb-16">
        <p className="text-xs font-black tracking-[.18em] text-reel uppercase">{eyebrow}</p>
        <h1 className="mt-3 text-4xl font-black tracking-tight">{title}</h1>
        <p className="mt-2 text-xs text-white/35">Versão 1.0 · atualizado em {updatedAt}</p>
        <div className="mt-10 space-y-8 text-sm leading-7 text-white/60 [&_a]:font-bold [&_a]:text-reel-bright [&_a]:underline [&_a]:underline-offset-4 [&_h2]:mb-3 [&_h2]:text-xl [&_h2]:font-black [&_h2]:text-white [&_ul]:list-disc [&_ul]:space-y-2 [&_ul]:pl-5">
          {children}
        </div>
      </article>
    </main>
  )
}
