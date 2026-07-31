export const serviceWorkerTemplate = `
const BUILD_VERSION = '__REELZ_BUILD_VERSION__'
const CACHE_PREFIX = 'reelz-'
const SHELL_CACHE = CACHE_PREFIX + 'shell-' + BUILD_VERSION
const STATIC_CACHE = CACHE_PREFIX + 'static-' + BUILD_VERSION
const SHELL_URLS = [
  '/index.html',
  '/manifest.webmanifest',
  '/icons/reelz-icon-192.png',
  '/icons/reelz-icon-512.png',
  '/icons/reelz-maskable-512.png',
  __REELZ_BUNDLE_ASSETS__
]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(SHELL_CACHE).then((cache) => cache.addAll(SHELL_URLS)),
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => Promise.all(
        cacheNames
          .filter((cacheName) => (
            cacheName.startsWith(CACHE_PREFIX)
            && cacheName !== SHELL_CACHE
            && cacheName !== STATIC_CACHE
          ))
          .map((cacheName) => caches.delete(cacheName)),
      ))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') {
    self.skipWaiting()
  }
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  if (request.method !== 'GET') return

  const url = new URL(request.url)
  if (url.origin !== self.location.origin || url.pathname.startsWith('/api/')) {
    return
  }

  if (request.mode === 'navigate') {
    event.respondWith(networkFirstNavigation(request))
    return
  }

  if (
    url.pathname.startsWith('/assets/')
    || url.pathname.startsWith('/icons/')
    || url.pathname === '/manifest.webmanifest'
  ) {
    event.respondWith(cacheFirst(request))
  }
})

async function networkFirstNavigation(request) {
  try {
    const response = await fetch(request)
    if (response.ok) {
      const cache = await caches.open(SHELL_CACHE)
      await cache.put('/index.html', response.clone())
    }
    return response
  } catch {
    return caches.match('/index.html')
  }
}

async function cacheFirst(request) {
  const cachedResponse = await caches.match(request)
  if (cachedResponse) return cachedResponse

  const response = await fetch(request)
  if (response.ok) {
    const cache = await caches.open(STATIC_CACHE)
    await cache.put(request, response.clone())
  }
  return response
}
`
