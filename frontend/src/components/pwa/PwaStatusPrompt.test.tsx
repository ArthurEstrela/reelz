import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  activateWaitingServiceWorker,
  PWA_UPDATE_AVAILABLE_EVENT,
} from '../../pwa/registerServiceWorker'
import { PWA_ENGAGEMENT_EVENT, PwaStatusPrompt } from './PwaStatusPrompt'

vi.mock('../../pwa/registerServiceWorker', () => ({
  PWA_UPDATE_AVAILABLE_EVENT: 'cinegiro:pwa-update-available',
  activateWaitingServiceWorker: vi.fn(),
}))

describe('PwaStatusPrompt', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('offers to activate a waiting application update', async () => {
    vi.mocked(activateWaitingServiceWorker).mockResolvedValue(true)
    const user = userEvent.setup()
    render(<PwaStatusPrompt />)

    act(() => {
      window.dispatchEvent(new Event(PWA_UPDATE_AVAILABLE_EVENT))
    })

    expect(screen.getByText('Nova versão disponível')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Atualizar' }))

    expect(activateWaitingServiceWorker).toHaveBeenCalledOnce()
  })

  it('uses the browser installation prompt when it becomes available', async () => {
    const prompt = vi.fn().mockResolvedValue(undefined)
    const installEvent = new Event('beforeinstallprompt', { cancelable: true })
    Object.assign(installEvent, {
      prompt,
      userChoice: Promise.resolve({ outcome: 'accepted' as const }),
    })
    const user = userEvent.setup()
    render(<PwaStatusPrompt />)

    act(() => {
      window.dispatchEvent(installEvent)
    })

    expect(screen.queryByText('Instale o CineGiro')).not.toBeInTheDocument()
    act(() => {
      window.dispatchEvent(new Event(PWA_ENGAGEMENT_EVENT))
      window.dispatchEvent(new Event(PWA_ENGAGEMENT_EVENT))
    })

    expect(screen.getByText('Instale o CineGiro')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Instalar' }))

    expect(prompt).toHaveBeenCalledOnce()
  })
})
