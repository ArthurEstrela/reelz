interface ReelzLogoProps {
  compact?: boolean
}

export function ReelzLogo({ compact = false }: ReelzLogoProps) {
  return (
    <div className="inline-flex items-center gap-3" aria-label="Reelz">
      <span className="grid size-10 place-items-center rounded-xl bg-reel shadow-[0_10px_30px_rgba(255,60,72,0.28)]">
        <svg viewBox="0 0 24 24" className="size-5 text-white" aria-hidden="true">
          <path
            fill="currentColor"
            d="M8.2 6.7c0-1.1 1.2-1.8 2.2-1.2l7.1 4.2a1.4 1.4 0 0 1 0 2.4l-7.1 4.2a1.4 1.4 0 0 1-2.2-1.2V6.7Z"
          />
        </svg>
      </span>
      {!compact && (
        <span className="text-xl font-black tracking-[-0.04em] text-white">reelz</span>
      )}
    </div>
  )
}
