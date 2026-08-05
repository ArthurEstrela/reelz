import { useEffect, useRef, useState, type ReactNode } from 'react'
import { NavLink } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
import { FeedbackDialog } from '../feedback/FeedbackDialog'
import { ReelzLogo } from '../brand/ReelzLogo'
import { NavigationIcon, type NavigationIconName } from './NavigationIcon'

interface AppHeaderProps {
  accessory?: ReactNode
}

const primaryItems: Array<{ to: string; label: string; icon: NavigationIconName; end?: boolean }> = [
  { to: '/', label: 'Roleta', icon: 'roulette', end: true },
  { to: '/social', label: 'Juntos', icon: 'social' },
  { to: '/library', label: 'Biblioteca', icon: 'library' },
]

export function AppHeader({ accessory }: AppHeaderProps) {
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const [feedbackOpen, setFeedbackOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!menuOpen) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMenuOpen(false)
    }
    const closeOnOutsideClick = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('keydown', closeOnEscape)
    document.addEventListener('pointerdown', closeOnOutsideClick)
    return () => {
      document.removeEventListener('keydown', closeOnEscape)
      document.removeEventListener('pointerdown', closeOnOutsideClick)
    }
  }, [menuOpen])

  const initial = user?.email.charAt(0).toUpperCase() || 'R'
  const navigationItems = user?.role === 'ADMIN'
    ? [...primaryItems, { to: '/admin/analytics', label: 'Analytics', icon: 'analytics' as const }]
    : primaryItems

  return (
    <>
      <header className="relative z-40 mx-auto grid w-full max-w-7xl grid-cols-[1fr_auto] items-center gap-4 lg:grid-cols-[1fr_auto_1fr]">
        <div className="justify-self-start">
          <ReelzLogo />
        </div>

        <nav aria-label="Navegação principal" className="hidden items-center gap-1 rounded-2xl border border-white/10 bg-surface/90 p-1.5 shadow-xl lg:flex">
          {navigationItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
                isActive ? 'bg-paper text-canvas' : 'text-white/55 hover:bg-white/[0.06] hover:text-white'
              }`}
            >
              <NavigationIcon name={item.icon} className="size-[1.1rem]" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center gap-2 justify-self-end">
          {accessory}
          <div className="relative" ref={menuRef}>
            <button
              type="button"
              aria-label="Abrir menu da conta"
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((current) => !current)}
              className="grid size-10 place-items-center rounded-full border border-white/12 bg-white/[0.055] text-sm font-bold text-paper transition hover:border-white/25 hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2"
            >
              {initial}
            </button>
            {menuOpen ? (
              <div className="absolute right-0 top-12 w-64 overflow-hidden rounded-2xl border border-white/10 bg-surface-raised p-2 shadow-2xl" role="menu">
                <div className="border-b border-white/8 px-3 py-3">
                  <p className="truncate text-sm font-semibold text-paper">{user?.email}</p>
                  <p className="mt-1 text-xs text-white/50">Conta Reelz</p>
                </div>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setMenuOpen(false)
                    setFeedbackOpen(true)
                  }}
                  className="mt-1 w-full rounded-xl px-3 py-2.5 text-left text-sm font-medium text-white/70 transition hover:bg-white/[0.06] hover:text-white"
                >
                  Enviar feedback
                </button>
                <button
                  type="button"
                  role="menuitem"
                  title={user?.email ? `Sair de ${user.email}` : 'Sair'}
                  onClick={logout}
                  className="w-full rounded-xl px-3 py-2.5 text-left text-sm font-medium text-white/70 transition hover:bg-white/[0.06] hover:text-white"
                >
                  Sair da conta
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </header>
      <FeedbackDialog open={feedbackOpen} onClose={() => setFeedbackOpen(false)} />
    </>
  )
}
