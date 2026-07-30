import { api } from './api'
import type { HistoryResponse, SaveHistoryRequest, WatchedMovie } from '../types/history'
import type { PageResponse } from '../types/pagination'

export async function saveHistory(payload: SaveHistoryRequest): Promise<HistoryResponse> {
  const { data } = await api.post<HistoryResponse>('/api/v1/history', payload)
  return data
}

export function markMovieAsWatched(movieId: number): Promise<HistoryResponse> {
  return saveHistory({ movieId, status: 'WATCHED' })
}

export async function getWatchedHistory(
  page: number,
  size = 24,
): Promise<PageResponse<WatchedMovie>> {
  const { data } = await api.get<PageResponse<WatchedMovie>>('/api/v1/history', {
    params: { page, size },
  })
  return data
}
