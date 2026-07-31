export interface OnboardingMovie {
  movieId: number
  title: string
  posterPath: string | null
  voteAverage: number | null
}

export interface OnboardingMoviesResponse {
  movies: OnboardingMovie[]
  targetCount: number
}

export interface CompleteOnboardingRequest {
  presentedMovieIds: number[]
  watchedMovieIds: number[]
}

export interface CompleteOnboardingResponse {
  onboardingCompleted: boolean
  watchedMoviesAdded: number
}
