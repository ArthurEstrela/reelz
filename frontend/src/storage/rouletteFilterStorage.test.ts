import { describe, expect, it } from 'vitest'
import {
  ROULETTE_FILTER_DRAFT_TTL_MS,
  clearRouletteFilterDraft,
  getRouletteFilterDraft,
  saveRouletteFilterDraft,
} from './rouletteFilterStorage'

const USER_ID = 'user-one'
const STORAGE_KEY = `cinegiro.roulette.filters.${USER_ID}`
const NOW = Date.UTC(2026, 8, 9, 12)

describe('rouletteFilterStorage', () => {
  it('keeps the latest optional filters for twelve hours', () => {
    saveRouletteFilterDraft(USER_ID, { genreId: 35, vibeId: 'light' }, NOW)

    expect(getRouletteFilterDraft(USER_ID, NOW + ROULETTE_FILTER_DRAFT_TTL_MS - 1)).toEqual({
      genreId: 35,
      vibeId: 'light',
      expiresAt: NOW + ROULETTE_FILTER_DRAFT_TTL_MS,
    })
  })

  it('removes an expired draft instead of restoring stale intent', () => {
    saveRouletteFilterDraft(USER_ID, { genreId: 27, vibeId: null }, NOW)

    expect(getRouletteFilterDraft(USER_ID, NOW + ROULETTE_FILTER_DRAFT_TTL_MS)).toBeNull()
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })

  it('isolates drafts by user and removes empty selections', () => {
    saveRouletteFilterDraft(USER_ID, { genreId: 18, vibeId: null }, NOW)

    expect(getRouletteFilterDraft('user-two', NOW)).toBeNull()
    saveRouletteFilterDraft(USER_ID, { genreId: null, vibeId: null }, NOW)
    expect(getRouletteFilterDraft(USER_ID, NOW)).toBeNull()

    clearRouletteFilterDraft(USER_ID)
  })

  it('discards malformed browser data safely', () => {
    localStorage.setItem(STORAGE_KEY, '{not-json')

    expect(getRouletteFilterDraft(USER_ID, NOW)).toBeNull()
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })
})
