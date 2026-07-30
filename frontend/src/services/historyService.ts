import { api } from './api'
import type { HistoryResponse, SaveHistoryRequest } from '../types/history'

export async function saveHistory(payload: SaveHistoryRequest): Promise<HistoryResponse> {
  const { data } = await api.post<HistoryResponse>('/api/v1/history', payload)
  return data
}

export function markMovieAsWatched(movieId: number): Promise<HistoryResponse> {
  return saveHistory({ movieId, status: 'WATCHED' })
}
