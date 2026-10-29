import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useMessageRealtimeStore } from '@/stores/messageRealtime'

const api = vi.hoisted(() => ({ count: vi.fn(), ticket: vi.fn() }))
const realtime = vi.hoisted(() => ({
  connections: [] as Array<{
    ticket: string
    callbacks: {
      onOpen?: () => void
      onMessage: (raw: string) => void
      onError: (error?: Event) => void
    }
    close: ReturnType<typeof vi.fn>
  }>,
  create: vi.fn(),
}))
vi.mock('@/api/system/message', () => ({ getUnreadMessageCount: api.count }))
vi.mock('@/api/system/realtime', () => ({ getNotificationRealtimeTicket: api.ticket }))
vi.mock('@/core/realtime/client', () => ({
  createRealtimeConnection: realtime.create,
  parseRealtimeEnvelope: (raw: string) => {
    try {
      const value = JSON.parse(raw)
      return value && typeof value.type === 'string' ? value : null
    } catch { return null }
  },
}))

describe('message realtime store', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    realtime.connections = []
    realtime.create.mockReset().mockImplementation((ticket, _transport, callbacks) => {
      const connection = { ticket: ticket.ticket, callbacks, close: vi.fn() }
      realtime.connections.push(connection)
      return { close: connection.close }
    })
    setActivePinia(createPinia())
    api.count.mockReset().mockResolvedValue({ data: 4 })
    api.ticket.mockReset().mockResolvedValue({ data: { ticket: 'one', path: '/stream', transport: 'SSE' } })
  })
  afterEach(() => { vi.useRealTimers() })

  it('uses authoritative counts, filters event kinds, and reconnects with a fresh ticket', async () => {
    const store = useMessageRealtimeStore()
    store.start('token:user:tenant')
    await vi.waitFor(() => expect(realtime.connections).toHaveLength(1))
    const first = realtime.connections[0]
    first.callbacks.onOpen?.()
    await vi.waitFor(() => expect(store.unreadCount).toBe(4))
    first.callbacks.onMessage(JSON.stringify({ type: 'realtime-test' }))
    expect(api.count).toHaveBeenCalledTimes(2)
    first.callbacks.onMessage(JSON.stringify({ type: 'system-message-state' }))
    await vi.waitFor(() => expect(api.count).toHaveBeenCalledTimes(3))
    first.callbacks.onError()
    expect(first.close).toHaveBeenCalled()
    api.ticket.mockResolvedValueOnce({ data: { ticket: 'fresh-ticket', path: '/stream', transport: 'SSE' } })
    await vi.advanceTimersByTimeAsync(1000)
    await vi.waitFor(() => expect(realtime.connections).toHaveLength(2))
    expect(api.ticket).toHaveBeenCalledTimes(2)
    expect(realtime.connections[1].ticket).toBe('fresh-ticket')
    store.stop()
  })

  it('aborts a pending ticket on logout and ignores its late response', async () => {
    let resolveTicket!: (value: unknown) => void
    api.ticket.mockReturnValueOnce(new Promise((resolve) => { resolveTicket = resolve }))
    const store = useMessageRealtimeStore()
    store.start('first-user')
    store.stop()
    resolveTicket({ data: { ticket: 'late', path: '/stream', transport: 'SSE' } })
    await Promise.resolve()
    expect(realtime.connections).toHaveLength(0)
    expect(store.unreadCount).toBe(0)
  })

  it('does not let an old count overwrite a newer identity count', async () => {
    let resolveOld!: (value: unknown) => void
    api.count.mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve }))
    const store = useMessageRealtimeStore()
    store.start('first-user')
    store.start('second-user')
    await vi.waitFor(() => expect(store.unreadCount).toBe(4))
    resolveOld({ data: 99 })
    await Promise.resolve()
    expect(store.unreadCount).toBe(4)
    store.stop()
  })

  it('ignores callbacks from identity A after switching to B', async () => {
    const store = useMessageRealtimeStore()
    store.start('identity-A')
    await vi.waitFor(() => expect(realtime.connections).toHaveLength(1))
    const connectionA = realtime.connections[0]
    connectionA.callbacks.onOpen?.()
    await vi.waitFor(() => expect(api.count).toHaveBeenCalledTimes(2))

    store.start('identity-B')
    await vi.waitFor(() => expect(realtime.connections).toHaveLength(2))
    const connectionB = realtime.connections[1]
    connectionB.callbacks.onOpen?.()
    await vi.waitFor(() => expect(api.count).toHaveBeenCalledTimes(4))
    const revisionAtB = store.revision
    const countCallsAtB = api.count.mock.calls.length

    connectionA.callbacks.onError()
    connectionA.callbacks.onOpen?.()
    connectionA.callbacks.onMessage(JSON.stringify({ type: 'system-message' }))
    await Promise.resolve()
    expect(connectionB.close).not.toHaveBeenCalled()
    expect(api.ticket).toHaveBeenCalledTimes(2)
    expect(api.count).toHaveBeenCalledTimes(countCallsAtB)
    expect(store.revision).toBe(revisionAtB)

    connectionB.callbacks.onMessage(JSON.stringify({ type: 'system-message-state' }))
    await vi.waitFor(() => expect(api.count).toHaveBeenCalledTimes(countCallsAtB + 1))
    expect(store.revision).toBe(revisionAtB + 1)
    expect(connectionB.close).not.toHaveBeenCalled()
    store.stop()
  })

  it('closes offline transport, pauses polling, and reconnects with a fresh ticket on online', async () => {
    const store = useMessageRealtimeStore()
    store.start('same-identity')
    await vi.waitFor(() => expect(realtime.connections).toHaveLength(1))
    const connectionA = realtime.connections[0]
    connectionA.callbacks.onOpen?.()
    await vi.waitFor(() => expect(api.count).toHaveBeenCalledTimes(2))
    const revisionBeforeOffline = store.revision
    const countCallsBeforeOffline = api.count.mock.calls.length

    api.ticket.mockResolvedValueOnce({ data: { ticket: 'fresh-online-ticket', path: '/stream', transport: 'SSE' } })
    window.dispatchEvent(new Event('offline'))
    expect(connectionA.close).toHaveBeenCalledOnce()
    connectionA.callbacks.onError()
    connectionA.callbacks.onOpen?.()
    connectionA.callbacks.onMessage(JSON.stringify({ type: 'system-message' }))
    await vi.advanceTimersByTimeAsync(120_000)
    expect(api.ticket).toHaveBeenCalledTimes(1)
    expect(api.count).toHaveBeenCalledTimes(countCallsBeforeOffline)
    expect(store.revision).toBe(revisionBeforeOffline)

    window.dispatchEvent(new Event('online'))
    await vi.waitFor(() => expect(realtime.connections).toHaveLength(2))
    const connectionB = realtime.connections[1]
    expect(connectionB.ticket).toBe('fresh-online-ticket')
    connectionB.callbacks.onOpen?.()
    await vi.waitFor(() => expect(api.count).toHaveBeenCalledTimes(countCallsBeforeOffline + 2))
    const revisionAtB = store.revision
    const countCallsAtB = api.count.mock.calls.length

    connectionA.callbacks.onError()
    connectionA.callbacks.onOpen?.()
    connectionA.callbacks.onMessage(JSON.stringify({ type: 'system-message-state' }))
    await Promise.resolve()
    expect(connectionB.close).not.toHaveBeenCalled()
    expect(api.ticket).toHaveBeenCalledTimes(2)
    expect(api.count).toHaveBeenCalledTimes(countCallsAtB)
    expect(store.revision).toBe(revisionAtB)

    connectionB.callbacks.onMessage(JSON.stringify({ type: 'system-message-state' }))
    await vi.waitFor(() => expect(api.count).toHaveBeenCalledTimes(countCallsAtB + 1))
    expect(store.revision).toBe(revisionAtB + 1)
    store.stop()
  })

  it('keeps one connection when start is repeated for the same identity', async () => {
    const store = useMessageRealtimeStore()
    store.start('same')
    store.start('same')
    await vi.waitFor(() => expect(realtime.connections).toHaveLength(1))
    expect(api.ticket).toHaveBeenCalledTimes(1)
    store.stop()
  })
})
