interface FormMessageProps {
  children: string
  tone?: 'error' | 'success'
}

export function FormMessage({ children, tone = 'error' }: FormMessageProps) {
  const isError = tone === 'error'
  return (
    <div
      role={isError ? 'alert' : 'status'}
      aria-live="polite"
      className={`flex gap-3 rounded-xl border px-4 py-3 text-sm leading-5 ${
        isError
          ? 'border-red-400/20 bg-red-400/8 text-red-200'
          : 'border-emerald-400/20 bg-emerald-400/8 text-emerald-200'
      }`}
    >
      <span className="mt-0.5 font-black" aria-hidden="true">{isError ? '!' : '✓'}</span>
      <span>{children}</span>
    </div>
  )
}
