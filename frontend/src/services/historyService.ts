import { api } from './api'
import type {
  HistoryResponse,
  LibraryMovie,
  SaveHistoryRequest,
  UserMovieStatus,
  WatchedMovie,
  WatchlistMovie,
} from '../types/history'
import type { PageResponse } from '../types/pagination'

export async function saveHistory(payload: SaveHistoryRequest): Promise<HistoryResponse> {
  const { data } = await api.post<HistoryResponse>('/api/v1/history', payload)
  return data
}

export function markMovieAsWatched(movieId: number): Promise<HistoryResponse> {
  return saveHistory({ movieId, status: 'WATCHED' })
}

export function saveMovieToWatchlist(movieId: number): Promise<HistoryResponse> {
  return saveHistory({ movieId, status: 'WATCHLIST' })
}

async function getHistory(
  status: UserMovieStatus,
  page: number,
  size: number,
): Promise<PageResponse<LibraryMovie>> {
  const { data } = await api.get<PageResponse<LibraryMovie>>('/api/v1/history', {
    params: { status, page, size },
  })
  return data
}

export async function getWatchedHistory(
  page: number,
  size = 24,
): Promise<PageResponse<WatchedMovie>> {
  return getHistory('WATCHED', page, size) as Promise<PageResponse<WatchedMovie>>
}

export async function getWatchlist(
  page: number,
  size = 24,
): Promise<PageResponse<WatchlistMovie>> {
  return getHistory('WATCHLIST', page, size) as Promise<PageResponse<WatchlistMovie>>
}

export async function removeMovieFromWatchlist(movieId: number): Promise<void> {
  await api.delete(`/api/v1/history/watchlist/${movieId}`)
}
