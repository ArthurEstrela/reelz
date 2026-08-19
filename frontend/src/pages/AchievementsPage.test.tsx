import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AchievementContext } from '../context/achievementContextDefinition'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import { AchievementsPage } from './AchievementsPage'

const authContext: AuthContextValue = {
  user: { id: 'user-id', email: 'collector@reelz.app', onboardingCompleted: true, role: 'USER' },
  isAuthenticated: true,
  login: vi.fn(),
  register: vi.fn(),
  markOnboardingCompleted: vi.fn(),
  logout: vi.fn(),
}

describe('AchievementsPage', () => {
  it('renders unlocked and in-progress achievements from the backend contract', () => {
    render(
      <AuthContext.Provider value={authContext}>
        <AchievementContext.Provider value={{
          loading: false,
          error: false,
          refreshAchievements: vi.fn(),
          overview: {
            unlockedCount: 1,
            totalCount: 2,
            achievements: [
              {
                code: 'FIRST_SPIN', name: 'Primeira Sessão', description: 'Encontre seu primeiro filme na roleta.', iconKey: 'ticket', category: 'DISCOVERY', target: 1, progress: 1, unlocked: true, unlockedAt: '2026-08-19T10:00:00Z',
              },
              {
                code: 'WATCHED_10', name: 'Arquivo Pessoal', description: 'Colecione 10 filmes que você já assistiu.', iconKey: 'film-stack', category: 'COLLECTION', target: 10, progress: 4, unlocked: false, unlockedAt: null,
              },
            ],
          },
        }}>
          <MemoryRouter initialEntries={['/achievements']}>
            <AchievementsPage />
          </MemoryRouter>
        </AchievementContext.Provider>
      </AuthContext.Provider>,
    )

    expect(screen.getByRole('heading', { name: 'Sala de Troféus' })).toBeInTheDocument()
    expect(screen.getByText('Primeira Sessão')).toBeInTheDocument()
    expect(screen.getByText('4/10')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
  })
})
