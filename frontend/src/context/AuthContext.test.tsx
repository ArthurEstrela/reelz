import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAuthSession } from '../storage/authStorage'
import { useAuth } from '../hooks/useAuth'
import { loginRequest, registerRequest } from '../services/authService'
import { AuthProvider } from './AuthContext'

vi.mock('../services/authService', () => ({
  loginRequest: vi.fn(),
  registerRequest: vi.fn(),
}))

const mockedLoginRequest = vi.mocked(loginRequest)
const mockedRegisterRequest = vi.mocked(registerRequest)

function AuthProbe() {
  const { user, login, register, logout } = useAuth()
  return (
    <div>
      <span>{user?.email ?? 'anonymous'}</span>
      <button type="button" onClick={() => void login('Person@Reelz.App', 'password-123')}>
        login
      </button>
      <button
        type="button"
        onClick={() =>
          void register({
            displayName: 'Person',
            email: 'person@reelz.app',
            password: 'password-123',
            termsAccepted: true,
            timezone: 'America/Sao_Paulo',
            countryCode: 'BR',
          })
        }
      >
        register
      </button>
      <button type="button" onClick={logout}>logout</button>
    </div>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => {
    mockedLoginRequest.mockReset()
    mockedRegisterRequest.mockReset()
  })

  it('stores the authenticated user after login and clears it on logout', async () => {
    mockedLoginRequest.mockResolvedValue({
      accessToken: 'signed-token',
      tokenType: 'Bearer',
      expiresInSeconds: 7_200,
      userId: 'user-id',
      onboardingCompleted: false,
      role: 'USER',
    })
    const user = userEvent.setup()
    render(<AuthProvider><AuthProbe /></AuthProvider>)

    await user.click(screen.getByRole('button', { name: 'login' }))

    expect(await screen.findByText('person@reelz.app')).toBeInTheDocument()
    expect(getAuthSession()?.accessToken).toBe('signed-token')
    expect(getAuthSession()?.user.onboardingCompleted).toBe(false)

    await user.click(screen.getByRole('button', { name: 'logout' }))
    expect(screen.getByText('anonymous')).toBeInTheDocument()
    expect(getAuthSession()).toBeNull()
  })

  it('delegates registration with the complete backend contract', async () => {
    mockedRegisterRequest.mockResolvedValue({
      id: 'user-id',
      displayName: 'Person',
      email: 'person@reelz.app',
      plan: 'FREE',
      role: 'USER',
      timezone: 'America/Sao_Paulo',
      countryCode: 'BR',
      onboardingCompleted: false,
      createdAt: '2026-07-29T15:00:00Z',
    })
    const user = userEvent.setup()
    render(<AuthProvider><AuthProbe /></AuthProvider>)

    await user.click(screen.getByRole('button', { name: 'register' }))

    expect(mockedRegisterRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        termsAccepted: true,
        timezone: 'America/Sao_Paulo',
        countryCode: 'BR',
      }),
    )
  })
})
