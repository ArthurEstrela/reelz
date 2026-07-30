export type UserMovieStatus = 'WATCHED' | 'WATCHLIST'

export interface SaveHistoryRequest {
  movieId: number
  status: UserMovieStatus
}

export interface HistoryResponse {
  id: string
  movieId: number
  status: UserMovieStatus
  watchedAt: string | null
  rating: number | null
  createdAt: string
  updatedAt: string
}

export interface WatchedMovie {
  id: string
  movieId: number
  title: string
  posterPath: string | null
  tmdbRating: number | null
  status: 'WATCHED'
  watchedAt: string | null
  rating: number | null
}
