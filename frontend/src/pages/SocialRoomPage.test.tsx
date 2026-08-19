import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../context/authContextDefinition'
import { getVibes } from '../services/catalogService'
import { getTodayUsage } from '../services/rouletteService'
import { getSocialRoom, updateSocialPreference } from '../services/socialService'
import type { SocialRoom } from '../types/social'
import { SocialRoomPage } from './SocialRoomPage'

vi.mock('../services/catalogService', () => ({ getVibes: vi.fn() }))
vi.mock('../services/rouletteService', () => ({ getTodayUsage: vi.fn() }))
vi.mock('../services/socialService', () => ({
  getSocialRoom: vi.fn(),
  updateSocialPreference: vi.fn(),
  leaveSocialRoom: vi.fn(),
  spinSocialRoom: vi.fn(),
}))
vi.mock('../services/historyService', () => ({ markMovieAsWatched: vi.fn() }))
vi.mock('../services/analyticsService', () => ({ trackProductEvent: vi.fn() }))
vi.mock('../hooks/useAchievements', () => ({
  useAchievements: () => ({ refreshAchievements: vi.fn() }),
}))

const hostId = '0198f032-7370-7000-8000-000000000101'
const guestId = '0198f032-7370-7000-8000-000000000102'

const room: SocialRoom = {
  id: '0198f032-7370-7000-8000-000000000100',
  type: 'COUPLE',
  status: 'OPEN',
  inviteCode: 'ABCD2345',
  hostUserId: hostId,
  hostDisplayName: 'Ana',
  currentUserHost: true,
  capacity: 2,
  members: [
    {
      userId: hostId,
      displayName: 'Ana',
      host: true,
      joinedAt: '2026-08-05T12:00:00Z',
      providers: [],
      selectedGenreIds: [],
      selectedVibeId: null,
      selectedVibeName: null,
      ready: false,
      preferenceUpdatedAt: null,
    },
    {
      userId: guestId,
      displayName: 'Bia',
      host: false,
      joinedAt: '2026-08-05T12:01:00Z',
      providers: [],
      selectedGenreIds: [],
      selectedVibeId: null,
      selectedVibeName: null,
      ready: false,
      preferenceUpdatedAt: null,
    },
  ],
  commonProviders: [{ id: 'provider-id', name: 'Netflix', logoPath: null }],
  lastMovie: null,
  lastSpinNumber: 0,
  updatedAt: '2026-08-05T12:01:00Z',
}

const authContext: AuthContextValue = {
  user: {
    id: hostId,
    email: 'ana@reelz.app',
    onboardingCompleted: true,
    role: 'USER',
  },
  isAuthenticated: true,
  login: vi.fn(),
  register: vi.fn(),
  markOnboardingCompleted: vi.fn(),
  logout: vi.fn(),
}

function renderRoom() {
  return render(
    <AuthContext.Provider value={authContext}>
      <MemoryRouter initialEntries={[`/social/rooms/${room.id}`]}>
        <Routes>
          <Route path="/social/rooms/:roomId" element={<SocialRoomPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('SocialRoomPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getSocialRoom).mockResolvedValue(room)
    vi.mocked(getTodayUsage).mockResolvedValue({
      unlimited: false,
      dailyLimit: 5,
      remainingDailySpins: 5,
      remainingRewardedSpins: 0,
    })
    vi.mocked(getVibes).mockResolvedValue([{ id: 'vibe-id', name: 'Para rir' }])
  })

  it('lets the current participant submit an independent guess before the host can spin', async () => {
    const user = userEvent.setup()
    const hostReadyRoom: SocialRoom = {
      ...room,
      members: room.members.map((member) => member.userId === hostId
        ? { ...member, selectedGenreIds: [35], ready: true }
        : member),
    }
    vi.mocked(updateSocialPreference).mockResolvedValue(hostReadyRoom)

    renderRoom()

    expect(await screen.findByRole('heading', { name: 'O que você topa assistir?' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Aguardando 2 palpite(s)' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Comédia' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar meu palpite' }))

    await waitFor(() => expect(updateSocialPreference).toHaveBeenCalledWith(room.id, {
      genreIds: [35],
      vibeId: null,
      ready: true,
    }))
    expect(await screen.findByText('✓ Palpite confirmado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Aguardando 1 palpite(s)' })).toBeDisabled()
  })
})
