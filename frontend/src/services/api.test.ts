import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import {
  AUTH_SESSION_EXPIRED_EVENT,
  getAuthSession,
  saveAuthSession,
} from '../storage/authStorage'
import { api } from './api'

function successfulResponse(config: InternalAxiosRequestConfig): AxiosResponse {
  return {
    data: {},
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  }
}

describe('api interceptors', () => {
  it('injects the stored JWT into the Authorization header', async () => {
    saveAuthSession({
      accessToken: 'signed-token',
      expiresAt: Date.now() + 60_000,
      user: { id: 'user-id', email: 'person@reelz.app' },
    })
    let authorization: string | undefined

    await api.get('/private', {
      adapter: async (config) => {
        authorization = config.headers.get('Authorization') as string | undefined
        return successfulResponse(config)
      },
    })

    expect(authorization).toBe('Bearer signed-token')
  })

  it('clears the session and emits an event after a 401 response', async () => {
    saveAuthSession({
      accessToken: 'expired-token',
      expiresAt: Date.now() + 60_000,
      user: { id: 'user-id', email: 'person@reelz.app' },
    })
    const eventListener = vi.fn()
    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, eventListener)

    await expect(
      api.get('/private', {
        adapter: async (config) => {
          const response: AxiosResponse = {
            data: {},
            status: 401,
            statusText: 'Unauthorized',
            headers: {},
            config,
          }
          throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, response)
        },
      }),
    ).rejects.toBeInstanceOf(AxiosError)

    expect(getAuthSession()).toBeNull()
    expect(eventListener).toHaveBeenCalledOnce()
    window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, eventListener)
  })
})
