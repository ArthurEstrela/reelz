import { describe, expect, it } from 'vitest'
import { consumeAuthSessionExpired, getAuthSession } from './authStorage'

describe('authStorage brand migration', () => {
  it('migrates a valid Reelz session without logging the user out', () => {
    const legacySession = {
      accessToken: 'legacy-token',
      expiresAt: Date.now() + 60_000,
      user: {
        id: 'user-id',
        email: 'person@example.com',
        onboardingCompleted: true,
        role: 'USER',
      },
    }
    sessionStorage.setItem('reelz.auth.session', JSON.stringify(legacySession))

    expect(getAuthSession()).toEqual(legacySession)
    expect(sessionStorage.getItem('cinegiro.auth.session')).toBe(JSON.stringify(legacySession))
    expect(sessionStorage.getItem('reelz.auth.session')).toBeNull()
  })

  it('consumes an expired-session marker created before the rename', () => {
    sessionStorage.setItem('reelz.auth.expired', 'true')

    expect(consumeAuthSessionExpired()).toBe(true)
    expect(sessionStorage.getItem('reelz.auth.expired')).toBeNull()
  })
})
