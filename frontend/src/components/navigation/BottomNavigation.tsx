import { NavLink } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
import { NavigationIcon, type NavigationIconName } from './NavigationIcon'

const items: Array<{ to: string; label: string; icon: NavigationIconName; end?: boolean }> = [
  { to: '/', label: 'Roleta', icon: 'roulette', end: true },
  { to: '/social', label: 'Juntos', icon: 'social' },
  { to: '/library', label: 'Biblioteca', icon: 'library' },
  { to: '/achievements', label: 'Troféus', icon: 'achievements' },
]

export function BottomNavigation() {
  const { user } = useAuth()
  const navigationItems = user?.role === 'ADMIN'
    ? [...items, { to: '/admin/analytics', label: 'Analytics', icon: 'analytics' as const }]
    : items

  return (
    <nav
      aria-label="Navegação principal móvel"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-white/10 bg-canvas/95 px-4 pt-2 pb-[calc(.65rem+env(safe-area-inset-bottom))] backdrop-blur-xl lg:hidden"
    >
      <div className="mx-auto grid max-w-md gap-1" style={{ gridTemplateColumns: `repeat(${navigationItems.length}, minmax(0, 1fr))` }}>
        {navigationItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => `flex min-h-12 flex-col items-center justify-center gap-1 rounded-xl px-2 py-1.5 text-[0.68rem] font-semibold transition ${
              isActive ? 'bg-brand/14 text-brand-bright' : 'text-white/50 hover:bg-white/[0.04] hover:text-white/75'
            }`}
          >
            <NavigationIcon name={item.icon} />
            {item.label}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
