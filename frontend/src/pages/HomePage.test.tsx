import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import { spinRoulette } from '../services/rouletteService'
import type { ApiErrorResponse } from '../types/api'
import type { RouletteSpinResponse } from '../types/roulette'
import { HomePage } from './HomePage'

vi.mock('../services/rouletteService', () => ({
  spinRoulette: vi.fn(),
}))

const PROVIDER_ID = '0198f032-7370-7000-8000-000000000001'

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
      <HomePage
        providerOptions={[{ value: PROVIDER_ID, label: 'Netflix', emoji: 'N' }]}
        vibeOptions={[]}
        minimumSpinDuration={0}
      />
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

describe('HomePage roulette', () => {
  it('sends the selected filters and reveals the movie with the updated quota', async () => {
    vi.mocked(spinRoulette).mockResolvedValueOnce(successfulSpin)
    const user = userEvent.setup()
    renderHome()

    await user.click(screen.getByRole('button', { name: 'Comédia' }))
    await user.click(screen.getByRole('button', { name: 'Girar Roleta' }))

    expect(await screen.findByRole('heading', { name: 'Clube da Luta' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Assistir na Netflix' })).toHaveAttribute(
      'href',
      'https://www.netflix.com/title/example',
    )
    expect(screen.getByLabelText('4 giros restantes hoje')).toBeInTheDocument()
    expect(spinRoulette).toHaveBeenCalledWith({
      idempotencyKey: expect.any(String),
      providerIds: [PROVIDER_ID],
      genreId: 35,
      vibeId: null,
    })
  })

  it('shows a playful filter hint when the API returns 404', async () => {
    vi.mocked(spinRoulette).mockRejectedValueOnce(apiError(404))
    const user = userEvent.setup()
    renderHome()

    await user.click(screen.getByRole('button', { name: 'Girar Roleta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('mudar os filtros')
  })

  it('opens the exhausted-spins modal when the API returns 429', async () => {
    vi.mocked(spinRoulette).mockRejectedValueOnce(apiError(429))
    const user = userEvent.setup()
    renderHome()

    await user.click(screen.getByRole('button', { name: 'Girar Roleta' }))

    expect(await screen.findByRole('dialog')).toHaveTextContent('Seus giros acabaram')
    expect(screen.getByLabelText('0 giros restantes hoje')).toBeInTheDocument()
  })
})
