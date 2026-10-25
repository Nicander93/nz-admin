import { beforeEach, describe, expect, it, vi } from 'vitest'

import { bindFileStorage, downloadFile } from '@/api/system/file'

const axiosMock = vi.hoisted(() => ({
  requestInterceptor: undefined as ((config: unknown) => unknown) | undefined,
  get: vi.fn(),
  put: vi.fn(),
}))

vi.mock('axios', () => ({
  default: {
    create: () => ({
      interceptors: {
        request: {
          use: (handler: (config: unknown) => unknown) => {
            axiosMock.requestInterceptor = handler
          },
        },
        response: { use: vi.fn() },
      },
      get: axiosMock.get,
      post: vi.fn(),
      put: axiosMock.put,
      delete: vi.fn(),
    }),
  },
}))

describe('file API', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('token', 'file-session-token')
    axiosMock.get.mockReset()
    axiosMock.put.mockReset()
    axiosMock.get.mockResolvedValue({ data: new Blob(['file']) })
    axiosMock.put.mockResolvedValue({ code: 200, msg: '', data: undefined })
  })

  it('downloads with blob response and the shared request auth interceptor', async () => {
    const response = await downloadFile(51)

    expect(axiosMock.get).toHaveBeenCalledWith(
      '/api/system/file/download/51',
      { responseType: 'blob' },
    )
    expect(response.data).toBeInstanceOf(Blob)
    const requestConfig = axiosMock.requestInterceptor?.({
      headers: {},
    }) as { headers: { Authorization?: string } }
    expect(requestConfig.headers.Authorization).toBe('file-session-token')
  })

  it('targets the historical storage binding endpoint', async () => {
    await bindFileStorage(9, 4)

    expect(axiosMock.put).toHaveBeenCalledWith('/api/system/file/9/storage-binding/4', undefined, undefined)
  })
})
