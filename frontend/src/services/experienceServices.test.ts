import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'
import { getProviders, getVibes } from './catalogService'
import {
  getWatchedHistory,
  getWatchlist,
  markMovieAsWatched,
  removeMovieFromWatchlist,
  saveMovieToWatchlist,
} from './historyService'
import { getTodayUsage } from './rouletteService'
import {
  createSocialRoom,
  getSocialRoom,
  joinSocialRoom,
  leaveSocialRoom,
  listSocialRooms,
  spinSocialRoom,
} from './socialService'

vi.mock('./api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

beforeEach(() => {
  vi.resetAllMocks()
})

describe('experience API services', () => {
  it('uses the authenticated catalog endpoints', async () => {
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: [{ id: 'provider-id', name: 'Netflix' }] })
      .mockResolvedValueOnce({ data: [{ id: 'vibe-id', name: 'Para rir' }] })

    await expect(getProviders()).resolves.toEqual([{ id: 'provider-id', name: 'Netflix' }])
    await expect(getVibes()).resolves.toEqual([{ id: 'vibe-id', name: 'Para rir' }])
    expect(api.get).toHaveBeenNthCalledWith(1, '/api/v1/catalog/providers')
    expect(api.get).toHaveBeenNthCalledWith(2, '/api/v1/catalog/vibes')
  })

  it('loads the current daily quota', async () => {
    const quota = {
      unlimited: false,
      dailyLimit: 5,
      remainingDailySpins: 3,
      remainingRewardedSpins: 0,
    }
    vi.mocked(api.get).mockResolvedValueOnce({ data: quota })

    await expect(getTodayUsage()).resolves.toEqual(quota)
    expect(api.get).toHaveBeenCalledWith('/api/v1/roulette/usage/today')
  })

  it('marks a TMDB movie as watched with the expected contract', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { id: 'history-id' } })

    await markMovieAsWatched(550)

    expect(api.post).toHaveBeenCalledWith('/api/v1/history', {
      movieId: 550,
      status: 'WATCHED',
    })
  })

  it('loads a page from the watched history', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: { content: [], page: { number: 2, size: 24, totalElements: 0, totalPages: 0 } },
    })

    await getWatchedHistory(2, 24)

    expect(api.get).toHaveBeenCalledWith('/api/v1/history', {
      params: { status: 'WATCHED', page: 2, size: 24 },
    })
  })

  it('saves, lists and removes watchlist movies with the expected contracts', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { id: 'watchlist-id' } })
    vi.mocked(api.get).mockResolvedValueOnce({
      data: { content: [], page: { number: 0, size: 24, totalElements: 0, totalPages: 0 } },
    })
    vi.mocked(api.delete).mockResolvedValueOnce({ data: undefined })

    await saveMovieToWatchlist(603)
    await getWatchlist(0, 24)
    await removeMovieFromWatchlist(603)

    expect(api.post).toHaveBeenCalledWith('/api/v1/history', {
      movieId: 603,
      status: 'WATCHLIST',
    })
    expect(api.get).toHaveBeenCalledWith('/api/v1/history', {
      params: { status: 'WATCHLIST', page: 0, size: 24 },
    })
    expect(api.delete).toHaveBeenCalledWith('/api/v1/history/watchlist/603')
  })

  it('uses the social room contracts for create, invite, polling, spin and leave', async () => {
    const room = { id: 'room-id', inviteCode: 'ABCD2345' }
    const spinRequest = {
      idempotencyKey: 'spin-key',
      providerIds: ['provider-id'],
      genreId: null,
      vibeId: null,
      sessionId: 'session-id',
    }
    vi.mocked(api.post)
      .mockResolvedValueOnce({ data: room })
      .mockResolvedValueOnce({ data: room })
      .mockResolvedValueOnce({ data: { room, movie: { tmdbId: 550 } } })
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: [room] })
      .mockResolvedValueOnce({ data: room })
    vi.mocked(api.delete).mockResolvedValueOnce({ data: undefined })

    await createSocialRoom('COUPLE')
    await joinSocialRoom('abcd2345')
    await listSocialRooms()
    await getSocialRoom('room-id')
    await spinSocialRoom('room-id', spinRequest)
    await leaveSocialRoom('room-id')

    expect(api.post).toHaveBeenNthCalledWith(1, '/api/v1/social/rooms', { type: 'COUPLE' })
    expect(api.post).toHaveBeenNthCalledWith(2, '/api/v1/social/rooms/join', { inviteCode: 'abcd2345' })
    expect(api.get).toHaveBeenNthCalledWith(1, '/api/v1/social/rooms')
    expect(api.get).toHaveBeenNthCalledWith(2, '/api/v1/social/rooms/room-id')
    expect(api.post).toHaveBeenNthCalledWith(3, '/api/v1/social/rooms/room-id/spin', spinRequest)
    expect(api.delete).toHaveBeenCalledWith('/api/v1/social/rooms/room-id/members/me')
  })
})
