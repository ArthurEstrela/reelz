export const PWA_UPDATE_AVAILABLE_EVENT = 'cinegiro:pwa-update-available'

let refreshing = false

function notifyUpdateAvailable() {
  window.dispatchEvent(new Event(PWA_UPDATE_AVAILABLE_EVENT))
}

function observeInstallingWorker(worker: ServiceWorker) {
  worker.addEventListener('statechange', () => {
    if (worker.state === 'installed' && navigator.serviceWorker.controller) {
      notifyUpdateAvailable()
    }
  })
}

export function registerServiceWorker(): void {
  if (!('serviceWorker' in navigator)) return
  let alreadyControlled = navigator.serviceWorker.controller !== null

  window.addEventListener('load', () => {
    void navigator.serviceWorker.register('/sw.js', { scope: '/' })
      .then((registration) => {
        if (registration.waiting) notifyUpdateAvailable()
        registration.addEventListener('updatefound', () => {
          if (registration.installing) observeInstallingWorker(registration.installing)
        })
      })
      .catch(() => {
        // A PWA é um aprimoramento progressivo: uma falha de registro não
        // deve impedir autenticação, roleta ou biblioteca.
      })
  })

  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (!alreadyControlled) {
      alreadyControlled = true
      return
    }
    if (refreshing) return
    refreshing = true
    window.location.reload()
  })
}

export async function activateWaitingServiceWorker(): Promise<boolean> {
  if (!('serviceWorker' in navigator)) return false
  const registration = await navigator.serviceWorker.getRegistration('/')
  if (!registration?.waiting) return false
  registration.waiting.postMessage({ type: 'SKIP_WAITING' })
  return true
}
