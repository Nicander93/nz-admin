import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { useRealtimeConsole } from '@/views/system/realtime/hooks'
import {
  getRealtimeStats,
  getRealtimeTicket,
  sendRealtimeTest,
} from '@/api/system/realtime'

vi.mock('@/api/system/realtime', () => ({
  getRealtimeTicket: vi.fn(),
  getRealtimeStats: vi.fn(),
  sendRealtimeTest: vi.fn(),
}))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn() } }))

class FakeEventSource {
  static instances: FakeEventSource[] = []
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null
  close = vi.fn()

  constructor(readonly url: string) {
    FakeEventSource.instances.push(this)
  }
}

class FakeWebSocket {
  static instances: FakeWebSocket[] = []
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null
  close = vi.fn()

  constructor(readonly url: string) {
    FakeWebSocket.instances.push(this)
  }
}

describe('useRealtimeConsole', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    FakeEventSource.instances = []
    FakeWebSocket.instances = []
    vi.stubGlobal('EventSource', FakeEventSource)
    vi.stubGlobal('WebSocket', FakeWebSocket)
    vi.mocked(getRealtimeStats).mockResolvedValue({
      data: {
        sseConnections: 1,
        webSocketConnections: 2,
        totalConnections: 3,
      },
    } as never)
  })

  it('uses a one-time ticket to connect SSE and records messages', async () => {
    vi.mocked(getRealtimeTicket).mockResolvedValue({
      data: {
        ticket: 'sse-ticket',
        transport: 'SSE',
        path: '/realtime/sse',
        expiresInSeconds: 30,
      },
    } as never)
    const realtime = useRealtimeConsole()

    await realtime.connect()
    const source = FakeEventSource.instances[0]
    expect(source.url).toContain('/realtime/sse?ticket=sse-ticket')
    source.onopen?.(new Event('open'))
    expect(realtime.status.value).toBe('connected')

    source.onmessage?.({
      data: JSON.stringify({
        id: 'm1',
        type: 'test',
        sentAt: '2026-08-11T10:00:00Z',
        payload: { message: 'hello' },
      }),
    } as MessageEvent)
    await nextTick()

    expect(realtime.events.value).toHaveLength(1)
    expect(realtime.events.value[0].type).toBe('test')
  })

  it('connects WebSocket and sends a user-scoped test message', async () => {
    vi.mocked(getRealtimeTicket).mockResolvedValue({
      data: {
        ticket: 'ws-ticket',
        transport: 'WEBSOCKET',
        path: '/realtime/ws',
        expiresInSeconds: 30,
      },
    } as never)
    vi.mocked(sendRealtimeTest).mockResolvedValue({ data: 1 } as never)
    const realtime = useRealtimeConsole()
    realtime.transport.value = 'WEBSOCKET'

    await realtime.connect()
    expect(FakeWebSocket.instances[0].url).toContain('/realtime/ws?ticket=ws-ticket')

    realtime.testMessage.value = 'ping me'
    await realtime.sendTest()

    expect(sendRealtimeTest).toHaveBeenCalledWith('ping me')
    expect(realtime.stats.value.totalConnections).toBe(3)
  })

  it('closes a failed SSE connection because consumed tickets cannot reconnect', async () => {
    vi.mocked(getRealtimeTicket).mockResolvedValue({
      data: {
        ticket: 'single-use',
        transport: 'SSE',
        path: '/realtime/sse',
        expiresInSeconds: 30,
      },
    } as never)
    const realtime = useRealtimeConsole()

    await realtime.connect()
    const source = FakeEventSource.instances[0]
    source.onerror?.(new Event('error'))

    expect(source.close).toHaveBeenCalled()
    expect(realtime.status.value).toBe('error')
  })
})
