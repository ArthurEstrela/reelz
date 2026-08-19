import type { ReactNode } from 'react'

interface AchievementIconProps {
  iconKey: string
  className?: string
}

export function AchievementIcon({ iconKey, className = 'size-7' }: AchievementIconProps) {
  const paths: Record<string, ReactNode> = {
    ticket: <><path d="M5 6.5h14v3a2.5 2.5 0 0 0 0 5v3H5v-3a2.5 2.5 0 0 0 0-5v-3Z" /><path d="M12 8.5v7" /></>,
    play: <><circle cx="12" cy="12" r="9" /><path d="m10 8.8 5 3.2-5 3.2V8.8Z" fill="currentColor" stroke="none" /></>,
    'film-stack': <><rect x="5" y="4" width="14" height="16" rx="2" /><path d="M8 4v16M16 4v16M5 8h3M16 8h3M5 16h3M16 16h3" /></>,
    membership: <><path d="M6 4h12v16l-6-3-6 3V4Z" /><path d="m9.5 10 1.6 1.6 3.4-3.4" /></>,
    archive: <><path d="M4 7h16v13H4V7Z" /><path d="M3 4h18v3H3zM9 11h6" /></>,
    bookmark: <path d="M7 4h10a1 1 0 0 1 1 1v15l-6-3-6 3V5a1 1 0 0 1 1-1Z" />,
    compass: <><circle cx="12" cy="12" r="9" /><path d="m15.5 8.5-2.1 4.9-4.9 2.1 2.1-4.9 4.9-2.1Z" /></>,
    hearts: <path d="M12 19s-7-4.4-7-9.1C5 6.8 8.8 5.2 12 8.3c3.2-3.1 7-1.5 7 1.6C19 14.6 12 19 12 19Z" />,
    people: <><circle cx="12" cy="8" r="3" /><circle cx="5.5" cy="10" r="2" /><circle cx="18.5" cy="10" r="2" /><path d="M6.5 20c.2-4 2-6 5.5-6s5.3 2 5.5 6M2.5 19c.1-2.8 1.3-4.3 3.5-4.5M21.5 19c-.1-2.8-1.3-4.3-3.5-4.5" /></>,
    calendar: <><rect x="4" y="5.5" width="16" height="14" rx="2" /><path d="M8 3.5v4M16 3.5v4M4 10h16M8 14h2M14 14h2" /></>,
  }

  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className={className} aria-hidden="true">
      {paths[iconKey] ?? paths.ticket}
    </svg>
  )
}
