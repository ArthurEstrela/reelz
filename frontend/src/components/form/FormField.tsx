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
        className={`h-13 w-full rounded-lg border border-white/12 bg-white/[0.035] px-4 text-[15px] text-paper outline-none transition placeholder:text-white/40 hover:border-white/22 focus:border-brand/80 focus:bg-white/[0.055] focus:ring-4 focus:ring-brand/10 ${className}`}
        {...inputProps}
      />
    </label>
  )
}
