import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import { getCurrentSubscription } from '../services/billingService'
import { createSocialRoom, listSocialRooms } from '../services/socialService'
import type { BillingSubscription } from '../types/billing'
import type { SocialRoom } from '../types/social'
import { SocialLobbyPage } from './SocialLobbyPage'

vi.mock('../services/billingService', () => ({
  getCurrentSubscription: vi.fn(),
}))

vi.mock('../services/socialService', () => ({
  createSocialRoom: vi.fn(),
  joinSocialRoom: vi.fn(),
  listSocialRooms: vi.fn(),
}))

const authContext: AuthContextValue = {
  user: {
    id: 'user-id',
    email: 'person@cinegiro.app',
    onboardingCompleted: true,
    role: 'USER',
  },
  isAuthenticated: true,
  login: vi.fn(),
  register: vi.fn(),
  markOnboardingCompleted: vi.fn(),
  logout: vi.fn(),
}

function subscription(premium: boolean): BillingSubscription {
  return {
    accountPlan: premium ? 'PREMIUM' : 'FREE',
    premium,
    planCode: premium ? 'PREMIUM_MONTHLY' : null,
    status: premium ? 'ACTIVE' : null,
    amountCents: premium ? 1290 : 0,
    currency: 'BRL',
    currentPeriodEnd: null,
    canceledAt: null,
    checkoutUrl: null,
    cancelable: premium,
  }
}

function groupRoom(): SocialRoom {
  return {
    id: 'room-id',
    type: 'GROUP',
    status: 'OPEN',
    inviteCode: 'ABCD2345',
    hostUserId: 'user-id',
    hostDisplayName: 'Pessoa',
    currentUserHost: true,
    capacity: 8,
    members: [],
    commonProviders: [],
    lastMovie: null,
    lastSpinNumber: 0,
    updatedAt: '2026-08-28T12:00:00Z',
  }
}

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location">{location.pathname}</output>
}

function renderLobby() {
  render(
    <AuthContext.Provider value={authContext}>
      <MemoryRouter initialEntries={['/social']}>
        <Routes>
          <Route path="/social" element={<SocialLobbyPage />} />
          <Route path="/premium" element={<div>Premium</div>} />
          <Route path="/social/rooms/:roomId" element={<div>Sala</div>} />
        </Routes>
        <LocationProbe />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('SocialLobbyPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listSocialRooms).mockResolvedValue([])
  })

  it('direciona uma conta Free ao Premium ao tentar criar uma sala em grupo', async () => {
    vi.mocked(getCurrentSubscription).mockResolvedValue(subscription(false))
    const user = userEvent.setup()
    renderLobby()

    const groupButton = (await screen.findByText('Modo grupo')).closest('button')
    expect(groupButton).not.toBeNull()
    expect(await screen.findByText('Conhecer Premium →')).toBeInTheDocument()
    await user.click(groupButton!)

    expect(createSocialRoom).not.toHaveBeenCalled()
    expect(screen.getByTestId('location')).toHaveTextContent('/premium')
  })

  it('permite que uma conta Premium crie uma sala em grupo', async () => {
    vi.mocked(getCurrentSubscription).mockResolvedValue(subscription(true))
    vi.mocked(createSocialRoom).mockResolvedValue(groupRoom())
    const user = userEvent.setup()
    renderLobby()

    const groupButton = (await screen.findByText('Modo grupo')).closest('button')
    expect(groupButton).not.toBeNull()
    await waitFor(() => expect(groupButton).toBeEnabled())
    await user.click(groupButton!)

    expect(createSocialRoom).toHaveBeenCalledWith('GROUP')
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/social/rooms/room-id'))
  })
})
