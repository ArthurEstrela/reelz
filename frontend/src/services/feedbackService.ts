import { api } from './api'

export interface BetaFeedbackRequest {
  score: number
  message?: string
}
export async function submitBetaFeedback(payload: BetaFeedbackRequest): Promise<void> {
  await api.post('/api/v1/feedback', payload)
}
