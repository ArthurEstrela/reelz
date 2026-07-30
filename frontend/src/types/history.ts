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
