import { render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { RouletteMovie } from '../../types/roulette'
import { MovieCard } from './MovieCard'

const movie: RouletteMovie = {
  id: 'movie-id',
  tmdbId: 550,
  title: 'Clube da Luta',
  overview: 'Sinopse',
  posterPath: null,
  releaseDate: '1999-10-15',
  tmdbRating: 8.4,
  streamingAvailability: [{
    providerId: 'provider-id',
    tmdbProviderId: 9,
    providerName: 'Prime Video',
    logoPath: 'https://cdn.movieofthenight.com/services/prime/video.svg',
    monetizationType: 'FLATRATE',
    attributionUrl: 'https://www.primevideo.com/detail/example',
    catalogSource: 'STREAMING_AVAILABILITY',
  }],
}

describe('MovieCard', () => {
  it('preserva a proporção de logos retangulares de streaming', () => {
    const { container } = render(
      <MovieCard
        movie={movie}
        onWatchedAndSpinAgain={vi.fn()}
        onSaveToWatchlist={vi.fn().mockResolvedValue(true)}
        onSpinAgain={vi.fn()}
        onWatchProvider={vi.fn()}
      />,
    )

    const logo = container.querySelector('a img')
    expect(logo).toHaveClass('object-contain')
    expect(logo?.parentElement).toHaveClass('w-12')
    expect(logo?.parentElement).toHaveClass('bg-black/20')
  })
})
