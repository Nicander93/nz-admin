import { beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'

import {
  getRealtimeStats,
  getRealtimeTicket,
  sendRealtimeTest,
  type RealtimeTicket,
} from '@/api/system/realtime'
import { useRealtimeConsole } from '@/views/system/realtime/hooks'

interface CapturedCallbacks {
  onOpen?: () => void
  onMessage: (raw: string) => void
  onError: (error?: Event) => void
}

const connectionMocks = vi.hoisted(() => ({ callbacks: [] as CapturedCallbacks[] }))

vi.mock('@/api/system/realtime', () => ({
  getRealtimeTicket: vi.fn(),
  getRealtimeStats: vi.fn(),
  sendRealtimeTest: vi.fn(),
}))
vi.mock('@/core/realtime/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/core/realtime/client')>()
  return {
    ...actual,
    createRealtimeConnection: (...args: Parameters<typeof actual.createRealtimeConnection>) => {
      connectionMocks.callbacks.push(args[2])
      return actual.createRealtimeConnection(...args)
    },
  }
})
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

function createTicket(ticket: string, transport: RealtimeTicket['transport'] = 'SSE'): { data: RealtimeTicket } {
  return {
    data: {
      ticket,
      transport,
      path: transport === 'SSE' ? '/realtime/sse' : '/realtime/ws',
      expiresInSeconds: 30,
    },
  }
}

function createDeferred<T>() {
  let resolve: (value: T) => void = () => undefined
  const promise = new Promise<T>((resolvePromise) => { resolve = resolvePromise })
  return { promise, resolve }
}

describe('useRealtimeConsole', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    connectionMocks.callbacks = []
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
    vi.mocked(getRealtimeTicket).mockResolvedValue(createTicket('sse-ticket') as never)
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
    realtime.disconnect()
  })

  it('connects WebSocket and sends a user-scoped test message', async () => {
    vi.mocked(getRealtimeTicket).mockResolvedValue(createTicket('ws-ticket', 'WEBSOCKET') as never)
    vi.mocked(sendRealtimeTest).mockResolvedValue({ data: 1 } as never)
    const realtime = useRealtimeConsole()
    realtime.transport.value = 'WEBSOCKET'

    await realtime.connect()
    expect(FakeWebSocket.instances[0].url).toContain('/realtime/ws?ticket=ws-ticket')

    realtime.testMessage.value = 'ping me'
    await realtime.sendTest()

    expect(sendRealtimeTest).toHaveBeenCalledWith('ping me')
    expect(realtime.stats.value.totalConnections).toBe(3)
    realtime.disconnect()
  })

  it('closes a failed SSE connection because consumed tickets cannot reconnect', async () => {
    vi.mocked(getRealtimeTicket).mockResolvedValue(createTicket('single-use') as never)
    const realtime = useRealtimeConsole()

    await realtime.connect()
    const source = FakeEventSource.instances[0]
    source.onerror?.(new Event('error'))

    expect(source.close).toHaveBeenCalled()
    expect(realtime.status.value).toBe('error')
  })

  it('aborts a pending ticket and ignores its late response after disconnect', async () => {
    const pending = createDeferred<{ data: RealtimeTicket }>()
    vi.mocked(getRealtimeTicket).mockReturnValue(pending.promise as never)
    const realtime = useRealtimeConsole()

    const connecting = realtime.connect()
    const signal = vi.mocked(getRealtimeTicket).mock.calls[0]?.[1]?.signal
    expect(signal).toBeDefined()

    realtime.disconnect()
    expect(signal?.aborted).toBe(true)
    pending.resolve(createTicket('late-ticket'))
    await connecting

    expect(FakeEventSource.instances).toHaveLength(0)
    expect(realtime.status.value).toBe('disconnected')
  })

  it('aborts pending ticket work when its Vue scope is disposed', async () => {
    const pending = createDeferred<{ data: RealtimeTicket }>()
    vi.mocked(getRealtimeTicket).mockReturnValue(pending.promise as never)
    const scope = effectScope()
    const realtime = scope.run(() => useRealtimeConsole())
    if (!realtime) throw new Error('Realtime console scope was not created')

    const connecting = realtime.connect()
    const signal = vi.mocked(getRealtimeTicket).mock.calls[0]?.[1]?.signal
    scope.stop()
    expect(signal?.aborted).toBe(true)

    pending.resolve(createTicket('unmounted-ticket'))
    await connecting
    expect(FakeEventSource.instances).toHaveLength(0)
    expect(realtime.status.value).toBe('disconnected')
  })

  it('ignores callbacks from a previous connection after reconnecting', async () => {
    vi.mocked(getRealtimeTicket).mockResolvedValueOnce(createTicket('first-ticket') as never)
    const realtime = useRealtimeConsole()
    await realtime.connect()
    const previousCallbacks = connectionMocks.callbacks[0]
    previousCallbacks.onOpen?.()
    expect(realtime.status.value).toBe('connected')

    const pending = createDeferred<{ data: RealtimeTicket }>()
    vi.mocked(getRealtimeTicket).mockReturnValueOnce(pending.promise as never)
    const reconnecting = realtime.connect()
    expect(realtime.status.value).toBe('connecting')

    previousCallbacks.onOpen?.()
    previousCallbacks.onMessage('stale message')
    previousCallbacks.onError(new Event('stale error'))
    expect(realtime.status.value).toBe('connecting')
    expect(realtime.events.value).toHaveLength(0)

    pending.resolve(createTicket('second-ticket'))
    await reconnecting
    const currentCallbacks = connectionMocks.callbacks[1]
    currentCallbacks.onOpen?.()
    currentCallbacks.onMessage(JSON.stringify({ id: 'new', type: 'test', sentAt: 'now', payload: {} }))

    previousCallbacks.onError(new Event('stale error'))
    previousCallbacks.onMessage('another stale message')
    expect(realtime.status.value).toBe('connected')
    expect(realtime.events.value.map((event) => event.id)).toEqual(['new'])
    realtime.disconnect()
  })

  it('cancels the pending ticket when the selected transport changes', async () => {
    const pending = createDeferred<{ data: RealtimeTicket }>()
    vi.mocked(getRealtimeTicket).mockReturnValue(pending.promise as never)
    const scope = effectScope()
    const realtime = scope.run(() => useRealtimeConsole())
    if (!realtime) throw new Error('Realtime console scope was not created')

    const connecting = realtime.connect()
    const signal = vi.mocked(getRealtimeTicket).mock.calls[0]?.[1]?.signal
    realtime.transport.value = 'WEBSOCKET'
    await nextTick()
    expect(signal?.aborted).toBe(true)
    expect(realtime.status.value).toBe('disconnected')

    pending.resolve(createTicket('stale-sse-ticket'))
    await connecting
    expect(FakeEventSource.instances).toHaveLength(0)
    expect(FakeWebSocket.instances).toHaveLength(0)
    scope.stop()
  })
})
