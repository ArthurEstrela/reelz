import type { PillOption } from '../components/roulette/FilterPills'

// O backend ainda não expõe um catálogo de gêneros. Estes são os IDs oficiais do TMDB.
export const GENRE_OPTIONS: PillOption<number>[] = [
  { value: 35, label: 'Comédia', emoji: '😄' },
  { value: 28, label: 'Ação', emoji: '💥' },
  { value: 27, label: 'Terror', emoji: '👻' },
  { value: 18, label: 'Drama', emoji: '🎭' },
  { value: 878, label: 'Ficção', emoji: '🚀' },
  { value: 53, label: 'Suspense', emoji: '🕵️' },
]
