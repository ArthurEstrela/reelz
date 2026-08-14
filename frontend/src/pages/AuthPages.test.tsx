import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import { AppRoutes } from '../routes/AppRoutes'
import type { ApiErrorResponse } from '../types/api'

function renderAt(path: string, overrides: Partial<AuthContextValue> = {}) {
  const context: AuthContextValue = {
    user: null,
    isAuthenticated: false,
    login: vi.fn(),
    register: vi.fn(),
    markOnboardingCompleted: vi.fn(),
    logout: vi.fn(),
    ...overrides,
  }

  render(
    <AuthContext.Provider value={context}>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
  return context
}

function invalidCredentialsError(): AxiosError<ApiErrorResponse> {
  const config = { headers: {} } as InternalAxiosRequestConfig
  const response: AxiosResponse<ApiErrorResponse> = {
    data: {
      timestamp: '2026-07-29T15:00:00Z',
      status: 401,
      error: 'Unauthorized',
      code: 'INVALID_CREDENTIALS',
      message: 'E-mail ou senha inválidos.',
      path: '/api/v1/auth/login',
      violations: [],
    },
    status: 401,
    statusText: 'Unauthorized',
    headers: {},
    config,
  }
  return new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, response)
}

describe('authentication pages', () => {
  it('shows the API message when login credentials are invalid', async () => {
    const login = vi.fn().mockRejectedValue(invalidCredentialsError())
    const user = userEvent.setup()
    renderAt('/login', { login })

    await user.type(screen.getByLabelText('E-mail'), 'person@reelz.app')
    await user.type(screen.getByLabelText('Senha'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Entrar no Reelz' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.')
  })

  it('registers with browser metadata and redirects to email verification', async () => {
    const register = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderAt('/register', { register })

    await user.type(screen.getByLabelText('Como podemos te chamar?'), 'Pessoa')
    await user.type(screen.getByLabelText('E-mail'), 'person@reelz.app')
    await user.type(screen.getByLabelText('Senha'), 'password-123')
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: 'Criar conta grátis' }))

    expect(await screen.findByText('Confira sua caixa de entrada.')).toBeInTheDocument()
    expect(screen.getByDisplayValue('person@reelz.app')).toBeInTheDocument()
    expect(register).toHaveBeenCalledWith(
      expect.objectContaining({
        displayName: 'Pessoa',
        email: 'person@reelz.app',
        termsAccepted: true,
        timezone: expect.any(String),
        countryCode: expect.stringMatching(/^[A-Z]{2}$/),
      }),
    )
  })
})
