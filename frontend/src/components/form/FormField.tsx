import type { InputHTMLAttributes } from 'react'

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
}

export function FormField({ label, id, className = '', ...inputProps }: FormFieldProps) {
  return (
    <label htmlFor={id} className="block">
      <span className="mb-2 block text-sm font-semibold text-white/72">{label}</span>
      <input
        id={id}
        className={`h-13 w-full rounded-xl border border-white/10 bg-white/[0.045] px-4 text-[15px] text-white outline-none transition placeholder:text-white/22 hover:border-white/18 focus:border-reel/80 focus:bg-white/[0.065] focus:ring-4 focus:ring-reel/10 ${className}`}
        {...inputProps}
      />
    </label>
  )
}
