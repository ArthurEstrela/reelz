import type { ButtonHTMLAttributes } from 'react'

interface SubmitButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean
  loadingLabel?: string
}

export function SubmitButton({
  children,
  loading = false,
  loadingLabel = 'Aguarde...',
  disabled,
  ...buttonProps
}: SubmitButtonProps) {
  return (
    <button
      type="submit"
      disabled={disabled || loading}
      className="group flex h-13 w-full items-center justify-center gap-2 rounded-lg bg-brand px-5 text-sm font-bold text-white shadow-[0_12px_32px_rgba(233,54,69,.18)] transition hover:bg-brand-bright hover:shadow-[0_15px_36px_rgba(233,54,69,.24)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand disabled:cursor-not-allowed disabled:opacity-55"
      {...buttonProps}
    >
      {loading && (
        <span className="size-4 animate-spin rounded-full border-2 border-white/35 border-t-white" aria-hidden="true" />
      )}
      <span>{loading ? loadingLabel : children}</span>
      {!loading && (
        <svg
          viewBox="0 0 24 24"
          className="size-4 transition-transform group-hover:translate-x-0.5"
          fill="none"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m9 18 6-6-6-6" />
        </svg>
      )}
    </button>
  )
}
