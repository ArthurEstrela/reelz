import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'
import { getProviders, getVibes } from './catalogService'
import { getWatchedHistory, markMovieAsWatched } from './historyService'
import { getTodayUsage } from './rouletteService'

vi.mock('./api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
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
      params: { page: 2, size: 24 },
    })
  })
})
