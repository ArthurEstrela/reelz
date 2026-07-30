import type { PillOption } from '../components/roulette/FilterPills'

interface EnvironmentOption {
  id: string
  label: string
  emoji?: string
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function parseEnvironmentOptions(rawValue: string | undefined): PillOption<string>[] {
  if (!rawValue) return []

  try {
    const parsed: unknown = JSON.parse(rawValue)
    if (!Array.isArray(parsed)) return []

    return parsed.flatMap((value): PillOption<string>[] => {
      if (typeof value !== 'object' || value === null) return []
      const option = value as Partial<EnvironmentOption>
      if (
        typeof option.id !== 'string' ||
        !UUID_PATTERN.test(option.id) ||
        typeof option.label !== 'string' ||
        !option.label.trim()
      ) {
        return []
      }
      return [{ value: option.id, label: option.label.trim(), emoji: option.emoji }]
    })
  } catch {
    return []
  }
}

const configuredProviders = parseEnvironmentOptions(import.meta.env.VITE_STREAMING_PROVIDERS)
const configuredVibes = parseEnvironmentOptions(import.meta.env.VITE_VIBES)

export const STREAMING_PROVIDER_OPTIONS: PillOption<string>[] = configuredProviders.length
  ? configuredProviders
  : [
      { value: 'netflix-unconfigured', label: 'Netflix', emoji: 'N', disabled: true },
      { value: 'prime-unconfigured', label: 'Prime Video', emoji: '▶', disabled: true },
      { value: 'max-unconfigured', label: 'Max', emoji: 'M', disabled: true },
      { value: 'disney-unconfigured', label: 'Disney+', emoji: '+', disabled: true },
    ]

export const GENRE_OPTIONS: PillOption<number>[] = [
  { value: 35, label: 'Comédia', emoji: '😄' },
  { value: 28, label: 'Ação', emoji: '💥' },
  { value: 27, label: 'Terror', emoji: '👻' },
  { value: 18, label: 'Drama', emoji: '🎭' },
  { value: 878, label: 'Ficção', emoji: '🚀' },
  { value: 53, label: 'Suspense', emoji: '🕵️' },
]

export const VIBE_OPTIONS: PillOption<string>[] = configuredVibes.length
  ? configuredVibes
  : [
      { value: 'fun-unconfigured', label: 'Para rir', emoji: '😂', disabled: true },
      { value: 'tension-unconfigured', label: 'Tensão', emoji: '😰', disabled: true },
      { value: 'comfort-unconfigured', label: 'Conforto', emoji: '🫶', disabled: true },
    ]
