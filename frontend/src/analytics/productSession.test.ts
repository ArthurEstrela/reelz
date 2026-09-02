import { describe, expect, it } from 'vitest'
import { getProductSessionId } from './productSession'

describe('productSession brand migration', () => {
  it('preserves the analytics session created before the rename', () => {
    sessionStorage.setItem('reelz.product.session', 'legacy-session-id')

    expect(getProductSessionId()).toBe('legacy-session-id')
    expect(sessionStorage.getItem('cinegiro.product.session')).toBe('legacy-session-id')
    expect(sessionStorage.getItem('reelz.product.session')).toBeNull()
  })
})
