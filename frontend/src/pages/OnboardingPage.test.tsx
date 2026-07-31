import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import {
  completeOnboarding,
  getOnboardingMovies,
} from '../services/onboardingService'
import { OnboardingPage } from './OnboardingPage'

vi.mock('../services/onboardingService', () => ({
  getOnboardingMovies: vi.fn(),
  completeOnboarding: vi.fn(),
}))

const markOnboardingCompleted = vi.fn()
const context: AuthContextValue = {
  user: {
    id: 'user-id',
    email: 'new@reelz.app',
    onboardingCompleted: false,
  },
  isAuthenticated: true,
  login: vi.fn(),
  register: vi.fn(),
  markOnboardingCompleted,
  logout: vi.fn(),
}

function renderPage() {
  render(
    <AuthContext.Provider value={context}>
      <MemoryRouter initialEntries={['/onboarding']}>
        <OnboardingPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('OnboardingPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getOnboardingMovies).mockResolvedValue({
      targetCount: 25,
      movies: [
        {
          movieId: 101,
          title: 'Filme conhecido',
          posterPath: '/known.jpg',
          voteAverage: 8.2,
        },
        {
          movieId: 202,
          title: 'Filme novo',
          posterPath: '/new.jpg',
          voteAverage: 7.4,
        },
      ],
    })
    vi.mocked(completeOnboarding).mockResolvedValue({
      onboardingCompleted: true,
      watchedMoviesAdded: 1,
    })
  })

  it('collects swipe decisions and persists only watched movies', async () => {
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByText('Filme conhecido')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Já assisti →' }))

    expect(await screen.findByText('Filme novo')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '← Não assisti' }))

    expect(await screen.findByText(/Marcamos 1 filme como assistido/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Começar a girar' }))

    await waitFor(() => {
      expect(completeOnboarding).toHaveBeenCalledWith({
        presentedMovieIds: [101, 202],
        watchedMovieIds: [101],
      })
    })
    expect(markOnboardingCompleted).toHaveBeenCalledOnce()
  })
})
