import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import { getProviders, getVibes } from '../services/catalogService'
import { markMovieAsWatched } from '../services/historyService'
import { getTodayUsage, spinRoulette } from '../services/rouletteService'
import type { ApiErrorResponse } from '../types/api'
import type { CatalogItem } from '../types/catalog'
import type { RouletteSpinResponse, SpinQuota } from '../types/roulette'
import { HomePage } from './HomePage'

vi.mock('../services/catalogService', () => ({
  getProviders: vi.fn(),
  getVibes: vi.fn(),
}))

vi.mock('../services/historyService', () => ({
  markMovieAsWatched: vi.fn(),
}))

vi.mock('../services/rouletteService', () => ({
  getTodayUsage: vi.fn(),
  spinRoulette: vi.fn(),
}))

const PROVIDER_ID = '0198f032-7370-7000-8000-000000000001'
const VIBE_ID = '0198f032-7370-7000-8000-000000000002'
const providers: CatalogItem[] = [{ id: PROVIDER_ID, name: 'Netflix' }]
const vibes: CatalogItem[] = [{ id: VIBE_ID, name: 'Para rir' }]

const initialQuota: SpinQuota = {
  unlimited: false,
  dailyLimit: 5,
  remainingDailySpins: 5,
  remainingRewardedSpins: 0,
}

const successfulSpin: RouletteSpinResponse = {
  movie: {
    id: '0198f032-7370-7000-8000-000000000010',
    tmdbId: 550,
    title: 'Clube da Luta',
    overview: 'Um homem insone encontra uma forma pouco convencional de mudar sua vida.',
    posterPath: '/poster.jpg',
    releaseDate: '1999-10-15',
    tmdbRating: 8.4,
    streamingAvailability: [
      {
        providerId: PROVIDER_ID,
        tmdbProviderId: 8,
        providerName: 'Netflix',
        logoPath: null,
        monetizationType: 'FLATRATE',
        attributionUrl: 'https://www.netflix.com/title/example',
      },
    ],
  },
  quota: {
    unlimited: false,
    dailyLimit: 5,
    remainingDailySpins: 4,
    remainingRewardedSpins: 0,
  },
}

const context: AuthContextValue = {
  user: { id: '0198f032-7370-7000-8000-000000000020', email: 'moviegoer@reelz.app' },
  isAuthenticated: true,
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}

function renderHome() {
  render(
    <AuthContext.Provider value={context}>
      <HomePage minimumSpinDuration={0} />
    </AuthContext.Provider>,
  )
}

function apiError(status: number): AxiosError<ApiErrorResponse> {
  const config = { headers: {} } as InternalAxiosRequestConfig
  const response: AxiosResponse<ApiErrorResponse> = {
    data: {
      timestamp: '2026-07-30T12:00:00Z',
      status,
      error: status === 404 ? 'Not Found' : 'Too Many Requests',
      code: status === 404 ? 'NO_MOVIES_FOUND' : 'DAILY_LIMIT_EXCEEDED',
      message: status === 404 ? 'Nenhum filme encontrado.' : 'Limite diário atingido.',
      path: '/api/v1/roulette/spin',
      violations: [],
    },
    status,
    statusText: String(status),
    headers: {},
    config,
  }
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, undefined, response)
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(getProviders).mockResolvedValue(providers)
  vi.mocked(getVibes).mockResolvedValue(vibes)
  vi.mocked(getTodayUsage).mockResolvedValue(initialQuota)
  vi.mocked(markMovieAsWatched).mockResolvedValue({
    id: 'history-id',
    movieId: 550,
    status: 'WATCHED',
    watchedAt: '2026-07-30T12:00:00Z',
    rating: null,
    createdAt: '2026-07-30T12:00:00Z',
    updatedAt: '2026-07-30T12:00:00Z',
  })
})

describe('HomePage roulette', () => {
  it('loads the official catalogs and the current quota on mount', async () => {
    renderHome()

    expect(screen.getByLabelText(/Carregando onde assistir/)).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Netflix' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Para rir' })).toBeInTheDocument()
    expect(await screen.findByLabelText('5 giros restantes hoje')).toBeInTheDocument()
    expect(getProviders).toHaveBeenCalledOnce()
    expect(getVibes).toHaveBeenCalledOnce()
    expect(getTodayUsage).toHaveBeenCalledOnce()
  })

  it('sends dynamic filters, reveals the movie and resynchronizes the quota', async () => {
    vi.mocked(spinRoulette).mockResolvedValueOnce(successfulSpin)
    vi.mocked(getTodayUsage)
      .mockResolvedValueOnce(initialQuota)
      .mockResolvedValueOnce(successfulSpin.quota)
    const user = userEvent.setup()
    renderHome()

    await screen.findByRole('button', { name: 'Netflix' })
    await user.click(screen.getByRole('button', { name: 'Comédia' }))
    await user.click(screen.getByRole('button', { name: 'Para rir' }))
    await user.click(screen.getByRole('button', { name: 'Girar Roleta' }))

    expect(await screen.findByRole('heading', { name: 'Clube da Luta' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Assistir na Netflix' })).toHaveAttribute(
      'href',
      'https://www.netflix.com/title/example',
    )
    expect(await screen.findByLabelText('4 giros restantes hoje')).toBeInTheDocument()
    expect(spinRoulette).toHaveBeenCalledWith({
      idempotencyKey: expect.any(String),
      providerIds: [PROVIDER_ID],
      genreId: 35,
      vibeId: VIBE_ID,
    })
    await waitFor(() => expect(getTodayUsage).toHaveBeenCalledTimes(2))
  })

  it('shows a playful filter hint when the spin returns 404', async () => {
    vi.mocked(spinRoulette).mockRejectedValueOnce(apiError(404))
    const user = userEvent.setup()
    renderHome()

    await screen.findByRole('button', { name: 'Netflix' })
    await user.click(screen.getByRole('button', { name: 'Girar Roleta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('mudar os filtros')
  })

  it('opens the exhausted-spins modal when the spin returns 429', async () => {
    vi.mocked(spinRoulette).mockRejectedValueOnce(apiError(429))
    const user = userEvent.setup()
    renderHome()

    await screen.findByRole('button', { name: 'Netflix' })
    await user.click(screen.getByRole('button', { name: 'Girar Roleta' }))

    expect(await screen.findByRole('dialog')).toHaveTextContent('Seus giros acabaram')
    expect(screen.getByLabelText('0 giros restantes hoje')).toBeInTheDocument()
  })

  it('starts another spin immediately and shows a toast if optimistic history fails', async () => {
    vi.mocked(spinRoulette)
      .mockResolvedValueOnce(successfulSpin)
      .mockImplementationOnce(() => new Promise(() => undefined))
    vi.mocked(markMovieAsWatched).mockRejectedValueOnce(new Error('offline'))
    const user = userEvent.setup()
    renderHome()

    await screen.findByRole('button', { name: 'Netflix' })
    await user.click(screen.getByRole('button', { name: 'Girar Roleta' }))
    await screen.findByRole('heading', { name: 'Clube da Luta' })
    await user.click(screen.getByRole('button', { name: 'Já vi / Girar de novo' }))

    expect(markMovieAsWatched).toHaveBeenCalledWith(550)
    expect(spinRoulette).toHaveBeenCalledTimes(2)
    expect(await screen.findByText('Procurando a sessão perfeita…')).toBeInTheDocument()
    expect(await screen.findByRole('alert')).toHaveTextContent('Não conseguimos marcar')
  })

  it('keeps filter skeletons visible while the catalog is pending', async () => {
    let resolveProviders!: (items: CatalogItem[]) => void
    let resolveVibes!: (items: CatalogItem[]) => void
    vi.mocked(getProviders).mockReturnValueOnce(new Promise((resolve) => { resolveProviders = resolve }))
    vi.mocked(getVibes).mockReturnValueOnce(new Promise((resolve) => { resolveVibes = resolve }))
    renderHome()

    expect(screen.getByLabelText(/Carregando onde assistir/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Carregando catálogo…' })).toBeDisabled()

    await act(async () => {
      resolveProviders(providers)
      resolveVibes(vibes)
    })
    expect(await screen.findByRole('button', { name: 'Netflix' })).toBeInTheDocument()
  })
})
