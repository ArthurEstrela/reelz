import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import { getAnalyticsOverview } from '../services/analyticsService'
import type { AnalyticsOverview } from '../types/analytics'
import { AdminAnalyticsPage } from './AdminAnalyticsPage'

vi.mock('../services/analyticsService', () => ({
  getAnalyticsOverview: vi.fn(),
}))

const overview: AnalyticsOverview = {
  from: '2026-07-04T00:00:00Z',
  to: '2026-08-03T00:00:00Z',
  totalUsers: 40,
  newUsers: 20,
  onboardingCompletedUsers: 16,
  firstSpinUsers: 14,
  activeUsers: 18,
  successfulSpins: 50,
  homeSessions: 30,
  decidedSessions: 10,
  providerClicks: 12,
  watchedMovies: 8,
  watchlistedMovies: 6,
  coupleModeInterestedUsers: 7,
  groupModeInterestedUsers: 4,
  d7EligibleUsers: 10,
  d7RetainedUsers: 3,
  activationRate: 80,
  decisionRate: 33.3,
  d7RetentionRate: 30,
  averageSpinsPerDecision: 2.4,
  feedbackCount: 1,
  averageFeedbackScore: 5,
  recentFeedback: [
    { createdAt: '2026-08-03T10:00:00Z', score: 5, message: 'Escolhi em segundos.' },
  ],
  daily: [
    { date: '2026-08-03', registrations: 2, successfulSpins: 5, decisions: 1 },
  ],
}
const authContext: AuthContextValue = {
  user: {
    id: 'admin-id',
    email: 'admin@reelz.app',
    onboardingCompleted: true,
    role: 'ADMIN',
  },
  isAuthenticated: true,
  login: vi.fn(),
  register: vi.fn(),
  markOnboardingCompleted: vi.fn(),
  logout: vi.fn(),
}

describe('AdminAnalyticsPage', () => {
  it('renders aggregate metrics and qualitative feedback', async () => {
    vi.mocked(getAnalyticsOverview).mockResolvedValue(overview)

    render(
      <AuthContext.Provider value={authContext}>
        <MemoryRouter>
          <AdminAnalyticsPage />
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(await screen.findByText('Escolhi em segundos.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Saúde do produto' })).toBeInTheDocument()
    expect(screen.getByText('7')).toBeInTheDocument()
    expect(screen.getByText('média / 5')).toBeInTheDocument()
  })
})
