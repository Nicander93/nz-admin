import { beforeEach, describe, expect, it, vi } from 'vitest'

import '@/api/request'

const mocks = vi.hoisted(() => ({
  onResponse: undefined as ((response: unknown) => Promise<unknown>) | undefined,
  onError: undefined as ((error: unknown) => Promise<unknown>) | undefined,
  logout: vi.fn(),
  replace: vi.fn(),
  errorMessage: vi.fn(),
}))

vi.mock('axios', () => ({
  default: {
    create: () => ({
      interceptors: {
        request: { use: vi.fn() },
        response: {
          use: (onResponse: typeof mocks.onResponse, onError: typeof mocks.onError) => {
            mocks.onResponse = onResponse
            mocks.onError = onError
          },
        },
      },
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    }),
  },
}))
vi.mock('element-plus', () => ({ ElMessage: { error: mocks.errorMessage } }))
vi.mock('@/router', () => ({ default: { replace: mocks.replace } }))
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({ token: 'active-token', logout: mocks.logout }),
}))

function responseHandler() {
  if (!mocks.onResponse) throw new Error('response interceptor missing')
  return mocks.onResponse
}

function errorHandler() {
  if (!mocks.onError) throw new Error('error interceptor missing')
  return mocks.onError
}

describe('request session lifecycle', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('token', 'active-token')
    mocks.logout.mockReset()
    mocks.replace.mockReset()
    mocks.errorMessage.mockReset()
  })

  it('clears the user store on a business JSON 401', async () => {
    await expect(responseHandler()({
      config: { headers: { Authorization: 'active-token' } },
      headers: {},
      data: { code: 401, msg: 'session expired' },
    })).rejects.toMatchObject({ code: 401 })

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledWith('/login')
  })

  it('clears the user store on a JSON error returned as a blob', async () => {
    await expect(responseHandler()({
      config: { responseType: 'blob', headers: { Authorization: 'active-token' } },
      headers: { 'content-type': 'application/json' },
      data: new Blob([JSON.stringify({ code: 401, msg: 'expired' })]),
    })).rejects.toMatchObject({ code: 401, msg: 'expired' })

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledWith('/login')
  })

  it('clears the user store on an HTTP 401', async () => {
    const error = {
      config: { headers: { Authorization: 'active-token' } },
      response: { status: 401, data: { msg: 'expired' } },
    }
    await expect(errorHandler()(error)).rejects.toBe(error)

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledWith('/login')
  })

  it.each(['json', 'blob', 'http'] as const)(
    'does not clear a replacement token when an old %s request returns 401 late',
    async (kind) => {
      localStorage.setItem('token', 'replacement-token')
      const oldConfig = {
        headers: { Authorization: 'active-token' },
        ...(kind === 'blob' ? { responseType: 'blob' } : {}),
      }

      if (kind === 'http') {
        await expect(errorHandler()({
          config: oldConfig,
          response: { status: 401 },
        })).rejects.toBeTruthy()
      } else {
        const data = kind === 'blob'
          ? new Blob([JSON.stringify({ code: 401, msg: 'expired' })])
          : { code: 401, msg: 'expired' }
        await expect(responseHandler()({
          config: oldConfig,
          headers: kind === 'blob' ? { 'content-type': 'application/json' } : {},
          data,
        })).rejects.toBeTruthy()
      }

      expect(localStorage.getItem('token')).toBe('replacement-token')
      expect(mocks.logout).not.toHaveBeenCalled()
      expect(mocks.replace).not.toHaveBeenCalled()
    },
  )

  it('only navigates once when unauthorized requests fail together', async () => {
    const handleError = errorHandler()
    const failures = await Promise.allSettled([
      handleError({ config: { headers: { Authorization: 'active-token' } }, response: { status: 401 } }),
      handleError({ config: { headers: { Authorization: 'active-token' } }, response: { status: 401 } }),
    ])

    expect(failures.every((result) => result.status === 'rejected')).toBe(true)
    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledOnce()
  })

  it('preserves the session on an HTTP 503', async () => {
    const error = {
      config: { headers: { Authorization: 'active-token' } },
      response: { status: 503 },
      message: 'unavailable',
    }
    await expect(errorHandler()(error)).rejects.toBe(error)

    expect(mocks.logout).not.toHaveBeenCalled()
    expect(mocks.replace).not.toHaveBeenCalled()
  })
})
