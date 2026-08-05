import type { PillOption } from '../components/roulette/FilterPills'

// O backend ainda não expõe um catálogo de gêneros. Estes são os IDs oficiais do TMDB.
export const GENRE_OPTIONS: PillOption<number>[] = [
  { value: 35, label: 'Comédia' },
  { value: 28, label: 'Ação' },
  { value: 27, label: 'Terror' },
  { value: 18, label: 'Drama' },
  { value: 878, label: 'Ficção' },
  { value: 53, label: 'Suspense' },
]
