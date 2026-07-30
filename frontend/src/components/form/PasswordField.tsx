import { useState, type InputHTMLAttributes } from 'react'

interface PasswordFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string
}

export function PasswordField({ label, id, ...inputProps }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)

  return (
    <label htmlFor={id} className="block">
      <span className="mb-2 block text-sm font-semibold text-white/72">{label}</span>
      <span className="relative block">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          className="h-13 w-full rounded-xl border border-white/10 bg-white/[0.045] px-4 pr-14 text-[15px] text-white outline-none transition placeholder:text-white/22 hover:border-white/18 focus:border-reel/80 focus:bg-white/[0.065] focus:ring-4 focus:ring-reel/10"
          {...inputProps}
        />
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          className="absolute inset-y-0 right-0 grid w-13 place-items-center text-white/35 transition hover:text-white/70 focus-visible:outline-2 focus-visible:outline-offset-[-4px] focus-visible:outline-reel"
          aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
        >
          {visible ? (
            <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" aria-hidden="true">
              <path strokeLinecap="round" strokeWidth="1.7" d="m4 4 16 16M10.6 10.7a2 2 0 0 0 2.7 2.7M9.9 5.2A9.7 9.7 0 0 1 12 5c5.5 0 9 7 9 7a15.8 15.8 0 0 1-2.2 3.2M6.6 6.6C4.3 8.2 3 10.6 3 12c0 0 3.5 7 9 7 1.4 0 2.7-.4 3.8-1" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" aria-hidden="true">
              <path strokeLinecap="round" strokeWidth="1.7" d="M3 12s3.5-7 9-7 9 7 9 7-3.5 7-9 7-9-7-9-7Z" />
              <circle cx="12" cy="12" r="2.6" strokeWidth="1.7" />
            </svg>
          )}
        </button>
      </span>
    </label>
  )
}
