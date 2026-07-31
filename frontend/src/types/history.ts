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

export interface LibraryMovie {
  id: string
  movieId: number
  title: string
  posterPath: string | null
  tmdbRating: number | null
  status: UserMovieStatus
  watchedAt: string | null
  rating: number | null
}

export type WatchedMovie = LibraryMovie & { status: 'WATCHED' }
export type WatchlistMovie = LibraryMovie & { status: 'WATCHLIST' }
