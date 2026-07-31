import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import { AppRoutes } from './AppRoutes'

function renderRoutes(
  isAuthenticated: boolean,
  initialPath = '/',
  onboardingCompleted = true,
) {
  const context: AuthContextValue = {
    user: isAuthenticated
      ? { id: 'user-id', email: 'person@reelz.app', onboardingCompleted }
      : null,
    isAuthenticated,
    login: vi.fn(),
    register: vi.fn(),
    markOnboardingCompleted: vi.fn(),
    logout: vi.fn(),
  }

  return render(
    <AuthContext.Provider value={context}>
      <MemoryRouter initialEntries={[initialPath]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('AppRoutes', () => {
  it('redirects an anonymous user from home to login', () => {
    renderRoutes(false)

    expect(screen.getByRole('heading', { name: 'Entre para girar.' })).toBeInTheDocument()
  })

  it('renders the private home for an authenticated user', () => {
    renderRoutes(true)

    expect(screen.getByRole('heading', { name: 'A um giro de distância' })).toBeInTheDocument()
    expect(screen.getByTitle('Sair de person@reelz.app')).toBeInTheDocument()
  })

  it('keeps authenticated users out of the login page', () => {
    renderRoutes(true, '/login')

    expect(screen.getByRole('heading', { name: 'A um giro de distância' })).toBeInTheDocument()
  })

  it('renders the private library for an authenticated user', () => {
    renderRoutes(true, '/library')

    expect(screen.getByRole('heading', { name: 'Biblioteca' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Navegação principal' })).toBeInTheDocument()
  })

  it('redirects a new authenticated user to onboarding', () => {
    renderRoutes(true, '/', false)

    expect(screen.getByRole('heading', { name: 'O que você já assistiu?' })).toBeInTheDocument()
  })
})
