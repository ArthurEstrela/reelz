export type ProductEventType =
  | 'HOME_VIEWED'
  | 'WATCH_PROVIDER_CLICKED'
  | 'COUPLE_MODE_INTERESTED'
  | 'GROUP_MODE_INTERESTED'

export interface ProductEventRequest {
  eventId: string
  sessionId: string
  eventType: ProductEventType
  movieId?: number
  providerId?: string
}

export interface DailyAnalytics {
  date: string
  registrations: number
  successfulSpins: number
  decisions: number
}

export interface RecentFeedback {
  createdAt: string
  score: number
  message: string | null
}

export interface AnalyticsOverview {
  from: string
  to: string
  totalUsers: number
  newUsers: number
  onboardingCompletedUsers: number
  firstSpinUsers: number
  activeUsers: number
  successfulSpins: number
  homeSessions: number
  decidedSessions: number
  providerClicks: number
  watchedMovies: number
  watchlistedMovies: number
  coupleModeInterestedUsers: number
  groupModeInterestedUsers: number
  socialRoomsCreated: number
  socialRoomsWithSpin: number
  socialSpins: number
  socialParticipants: number
  d7EligibleUsers: number
  d7RetainedUsers: number
  activationRate: number
  decisionRate: number
  d7RetentionRate: number
  averageSpinsPerDecision: number
  feedbackCount: number
  averageFeedbackScore: number
  recentFeedback: RecentFeedback[]
  daily: DailyAnalytics[]
}
