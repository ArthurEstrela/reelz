import { api } from './api'
import type {
  StreamingPreferences,
  UpdateStreamingPreferencesRequest,
} from '../types/streamingPreferences'

const PREFERENCES_ENDPOINT = '/api/v1/users/me/streaming-preferences'

export async function getStreamingPreferences(): Promise<StreamingPreferences> {
  const { data } = await api.get<StreamingPreferences>(PREFERENCES_ENDPOINT)
  return data
}

export async function updateStreamingPreferences(
  payload: UpdateStreamingPreferencesRequest,
): Promise<StreamingPreferences> {
  const { data } = await api.put<StreamingPreferences>(PREFERENCES_ENDPOINT, payload)
  return data
}
