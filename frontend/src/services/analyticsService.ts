import { getProductSessionId } from '../analytics/productSession'
import type {
  AnalyticsOverview,
  ProductEventRequest,
  ProductEventType,
} from '../types/analytics'
import { api } from './api'

interface EventDetails {
  movieId?: number
  providerId?: string
}

export async function trackProductEvent(
  eventType: ProductEventType,
  details: EventDetails = {},
): Promise<void> {
  const payload: ProductEventRequest = {
    eventId: crypto.randomUUID(),
    sessionId: getProductSessionId(),
    eventType,
    ...details,
  }
  await api.post('/api/v1/analytics/events', payload)
}

export function trackProductEventInBackground(
  eventType: ProductEventType,
  details: EventDetails = {},
): void {
  void trackProductEvent(eventType, details).catch(() => {
    // Analytics nunca deve bloquear a ação principal do usuário.
  })
}

export async function getAnalyticsOverview(days: number): Promise<AnalyticsOverview> {
  const { data } = await api.get<AnalyticsOverview>('/api/v1/admin/analytics/overview', {
    params: { days },
  })
  return data
}
