import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import {
  getWatchedHistory,
  getWatchlist,
  markMovieAsWatched,
  removeMovieFromWatchlist,
} from '../services/historyService'
import type { WatchlistMovie, WatchedMovie } from '../types/history'
import type { PageResponse } from '../types/pagination'
import { LibraryPage } from './LibraryPage'

vi.mock('../services/historyService', () => ({
  getWatchedHistory: vi.fn(),
  getWatchlist: vi.fn(),
  markMovieAsWatched: vi.fn(),
  removeMovieFromWatchlist: vi.fn(),
}))

const context: AuthContextValue = {
  user: { id: 'user-id', email: 'collector@reelz.app', onboardingCompleted: true, role: 'USER' },
  isAuthenticated: true,
  login: vi.fn(),
  register: vi.fn(),
  markOnboardingCompleted: vi.fn(),
  logout: vi.fn(),
}

const movies: WatchedMovie[] = [
  {
    id: 'history-1',
    movieId: 550,
    title: 'Clube da Luta',
    posterPath: '/fight-club.jpg',
    tmdbRating: 8.4,
    status: 'WATCHED',
    watchedAt: '2026-07-30T12:00:00Z',
    rating: null,
  },
  {
    id: 'history-2',
    movieId: 551,
    title: 'Matrix',
    posterPath: '/matrix.jpg',
    tmdbRating: 8.2,
    status: 'WATCHED',
    watchedAt: '2026-07-29T12:00:00Z',
    rating: 5,
  },
]

const watchlistMovies: WatchlistMovie[] = [
  {
    id: 'watchlist-1',
    movieId: 603,
    title: 'Matrix',
    posterPath: '/matrix.jpg',
    tmdbRating: 8.2,
    status: 'WATCHLIST',
    watchedAt: null,
    rating: null,
  },
  {
    id: 'watchlist-2',
    movieId: 680,
    title: 'Pulp Fiction',
    posterPath: '/pulp-fiction.jpg',
    tmdbRating: 8.5,
    status: 'WATCHLIST',
    watchedAt: null,
    rating: null,
  },
]

function page(content: WatchedMovie[], number: number, last: boolean): PageResponse<WatchedMovie> {
  return {
    content,
    page: {
      number,
      size: 1,
      totalElements: 2,
      totalPages: last ? number + 1 : 2,
    },
  }
}

function renderLibrary() {
  render(
    <AuthContext.Provider value={context}>
      <MemoryRouter initialEntries={['/library']}>
        <LibraryPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(markMovieAsWatched).mockResolvedValue({
    id: 'history-id',
    movieId: 680,
    status: 'WATCHED',
    watchedAt: '2026-07-30T12:00:00Z',
    rating: null,
    createdAt: '2026-07-30T12:00:00Z',
    updatedAt: '2026-07-30T12:00:00Z',
  })
  vi.mocked(removeMovieFromWatchlist).mockResolvedValue()
})

describe('LibraryPage', () => {
  it('shows the collection count and incrementally loads more posters', async () => {
    vi.mocked(getWatchedHistory)
      .mockResolvedValueOnce(page([movies[0]], 0, false))
      .mockResolvedValueOnce(page([movies[1]], 1, true))
    const user = userEvent.setup()
    renderLibrary()

    expect(screen.getByLabelText('Carregando biblioteca')).toBeInTheDocument()
    expect(await screen.findByText('Você já colecionou 2 filmes.')).toBeInTheDocument()
    expect(screen.getByAltText('Pôster de Clube da Luta')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Carregar mais' }))

    expect(await screen.findByAltText('Pôster de Matrix')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Carregar mais' })).not.toBeInTheDocument()
    expect(getWatchedHistory).toHaveBeenNthCalledWith(1, 0, 24)
    expect(getWatchedHistory).toHaveBeenNthCalledWith(2, 1, 24)
  })

  it('shows a collector empty state when no watched movies exist', async () => {
    vi.mocked(getWatchedHistory).mockResolvedValueOnce({
      ...page([], 0, true),
      page: {
        number: 0,
        size: 24,
        totalElements: 0,
        totalPages: 0,
      },
    })
    renderLibrary()

    expect(await screen.findByRole('heading', { name: 'Sua coleção começa no próximo giro' }))
      .toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Girar agora' })).toHaveAttribute('href', '/')
  })

  it('lists watchlist movies and supports optimistic removal and status change', async () => {
    vi.mocked(getWatchedHistory).mockResolvedValueOnce({
      ...page([], 0, true),
      page: { number: 0, size: 24, totalElements: 0, totalPages: 0 },
    })
    vi.mocked(getWatchlist).mockResolvedValueOnce({
      content: watchlistMovies,
      page: { number: 0, size: 24, totalElements: 2, totalPages: 1 },
    })
    const user = userEvent.setup()
    renderLibrary()

    await screen.findByRole('heading', { name: 'Sua coleção começa no próximo giro' })
    await user.click(screen.getByRole('tab', { name: 'Quero Ver' }))

    expect(await screen.findByText('2 filmes esperando por você.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', {
      name: 'Remover Matrix da lista Quero Ver',
    }))
    expect(removeMovieFromWatchlist).toHaveBeenCalledWith(603)
    await waitFor(() => {
      expect(screen.queryByAltText('Pôster de Matrix')).not.toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', {
      name: 'Marcar Pulp Fiction como assistido',
    }))
    expect(markMovieAsWatched).toHaveBeenCalledWith(680)
    expect(await screen.findByRole('heading', {
      name: 'Sua lista Quero Ver está livre',
    })).toBeInTheDocument()
  })
})
