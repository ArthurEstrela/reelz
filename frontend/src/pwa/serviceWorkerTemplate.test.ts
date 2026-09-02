import { describe, expect, it } from 'vitest'
import { serviceWorkerTemplate } from '../../serviceWorkerTemplate'

describe('serviceWorkerTemplate', () => {
  it('never intercepts API or cross-origin requests', () => {
    expect(serviceWorkerTemplate).toContain("url.pathname.startsWith('/api/')")
    expect(serviceWorkerTemplate).toContain('url.origin !== self.location.origin')
  })

  it('uses a build placeholder and removes current and legacy brand caches on activation', () => {
    expect(serviceWorkerTemplate).toContain('__CINEGIRO_BUILD_VERSION__')
    expect(serviceWorkerTemplate).toContain('__CINEGIRO_BUNDLE_ASSETS__')
    expect(serviceWorkerTemplate).toContain('cacheName.startsWith(CACHE_PREFIX)')
    expect(serviceWorkerTemplate).toContain('cacheName.startsWith(LEGACY_CACHE_PREFIX)')
    expect(serviceWorkerTemplate).toContain('caches.delete(cacheName)')
  })
})
