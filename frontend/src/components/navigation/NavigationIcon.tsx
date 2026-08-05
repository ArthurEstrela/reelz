import type { ReactNode } from 'react'

export type NavigationIconName = 'roulette' | 'social' | 'library' | 'analytics'

interface NavigationIconProps {
  name: NavigationIconName
  className?: string
}

export function NavigationIcon({ name, className = 'size-5' }: NavigationIconProps) {
  const paths: Record<NavigationIconName, ReactNode> = {
    roulette: (
      <>
        <circle cx="12" cy="12" r="8.25" />
        <path d="m10 8.8 5 3.2-5 3.2V8.8Z" fill="currentColor" stroke="none" />
      </>
    ),
    social: (
      <>
        <circle cx="8.5" cy="9" r="2.75" />
        <circle cx="16.5" cy="8" r="2.25" />
        <path d="M3.5 19c.25-3.4 2.1-5.3 5-5.3s4.75 1.9 5 5.3M14 13.2c3.6-.35 5.8 1.55 6.1 4.8" />
      </>
    ),
    library: (
      <>
        <rect x="4" y="4" width="16" height="16" rx="2" />
        <path d="M8 4v16M12 8h4M12 12h4M12 16h3" />
      </>
    ),
    analytics: (
      <>
        <path d="M5 19V11M12 19V5M19 19v-9" />
        <path d="M3 19h18" />
      </>
    ),
  }

  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      {paths[name]}
    </svg>
  )
}
