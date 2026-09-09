interface CineGiroLogoProps {
  compact?: boolean
}

export function CineGiroLogo({ compact = false }: CineGiroLogoProps) {
  return (
    <div className="inline-flex items-center" role="img" aria-label="CineGiro">
      <img
        src={compact ? '/brand/cinegiro-symbol.svg' : '/brand/cinegiro-lockup-dark.svg'}
        alt=""
        aria-hidden="true"
        draggable={false}
        className={compact ? 'size-10 select-none' : 'h-10 w-auto select-none'}
      />
    </div>
  )
}
