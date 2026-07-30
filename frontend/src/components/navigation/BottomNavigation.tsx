import { NavLink } from 'react-router-dom'

const items = [
  {
    to: '/',
    label: 'Roleta',
    end: true,
    icon: (
      <path fill="currentColor" d="M8 6.7c0-1.2 1.3-1.9 2.3-1.3l7 4.1a1.7 1.7 0 0 1 0 2.9l-7 4.2A1.5 1.5 0 0 1 8 15.3V6.7Z" />
    ),
  },
  {
    to: '/library',
    label: 'Biblioteca',
    end: false,
    icon: (
      <path fill="currentColor" d="M5.5 3A2.5 2.5 0 0 0 3 5.5v13A2.5 2.5 0 0 0 5.5 21h13a2.5 2.5 0 0 0 2.5-2.5v-13A2.5 2.5 0 0 0 18.5 3h-13Zm1.75 3.5a1.25 1.25 0 1 1 0 2.5 1.25 1.25 0 0 1 0-2.5Zm0 4.25a1.25 1.25 0 1 1 0 2.5 1.25 1.25 0 0 1 0-2.5Zm0 4.25a1.25 1.25 0 1 1 0 2.5 1.25 1.25 0 0 1 0-2.5ZM11 7h6v2h-6V7Zm0 4.25h6v2h-6v-2Zm0 4.25h6v2h-6v-2Z" />
    ),
  },
] as const

export function BottomNavigation() {
  return (
    <nav
      aria-label="Navegação principal"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-white/10 bg-canvas/90 px-4 pt-2 pb-[calc(.75rem+env(safe-area-inset-bottom))] backdrop-blur-xl"
    >
      <div className="mx-auto grid max-w-sm grid-cols-2 gap-2">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => `flex flex-col items-center gap-1 rounded-2xl px-4 py-2 text-[11px] font-extrabold transition ${
              isActive ? 'bg-reel/12 text-reel-bright' : 'text-white/35 hover:bg-white/[0.04] hover:text-white/70'
            }`}
          >
            <svg viewBox="0 0 24 24" className="size-5" aria-hidden="true">
              {item.icon}
            </svg>
            {item.label}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
