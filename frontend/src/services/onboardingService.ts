import { api } from './api'
import type {
  CompleteOnboardingRequest,
  CompleteOnboardingResponse,
  OnboardingMoviesResponse,
} from '../types/onboarding'

export async function getOnboardingMovies(limit = 25): Promise<OnboardingMoviesResponse> {
  const { data } = await api.get<OnboardingMoviesResponse>('/api/v1/onboarding/movies', {
    params: { limit },
  })
  return data
}

export async function completeOnboarding(
  payload: CompleteOnboardingRequest,
): Promise<CompleteOnboardingResponse> {
  const { data } = await api.post<CompleteOnboardingResponse>(
    '/api/v1/onboarding/complete',
    payload,
  )
  return data
}
