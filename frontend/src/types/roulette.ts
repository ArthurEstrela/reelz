export interface RouletteSpinRequest {
  idempotencyKey: string
  providerIds: string[]
  genreId: number | null
  vibeId: string | null
  sessionId: string
}

export interface StreamingAvailability {
  providerId: string
  tmdbProviderId: number
  providerName: string
  logoPath: string | null
  monetizationType: 'FLATRATE' | 'FREE' | 'ADS' | 'RENT' | 'BUY'
  attributionUrl: string | null
}

export interface RouletteMovie {
  id: string
  tmdbId: number
  title: string
  overview: string | null
  posterPath: string | null
  releaseDate: string | null
  tmdbRating: number | null
  streamingAvailability: StreamingAvailability[]
}

export interface SpinQuota {
  unlimited: boolean
  dailyLimit: number | null
  remainingDailySpins: number | null
  remainingRewardedSpins: number
}

export interface RouletteSpinResponse {
  movie: RouletteMovie
  quota: SpinQuota
}
