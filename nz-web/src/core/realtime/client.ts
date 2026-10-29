import type {
  RealtimeEnvelope,
  RealtimeTicket,
  RealtimeTransport,
} from '@/api/system/realtime'

export interface RealtimeConnectionCallbacks {
  onOpen?: () => void
  onMessage: (raw: string) => void
  onError: (error?: Event) => void
}

export interface RealtimeConnectionHandle {
  close(): void
}

/** Opens an already-ticketed connection. Each failed connection is closed so an expired one-use ticket is never reused. */
export function createRealtimeConnection(
  ticket: RealtimeTicket,
  transport: RealtimeTransport,
  callbacks: RealtimeConnectionCallbacks,
): RealtimeConnectionHandle {
  const url = new URL(ticket.path, window.location.origin)
  url.searchParams.set('ticket', ticket.ticket)
  let explicitlyClosed = false
  let failed = false

  if (transport === 'SSE') {
    const source = new EventSource(url.toString())
    source.onopen = () => {
      if (!explicitlyClosed && !failed) callbacks.onOpen?.()
    }
    source.onmessage = (event) => {
      if (!explicitlyClosed && !failed) callbacks.onMessage(event.data)
    }
    source.onerror = (event) => {
      if (explicitlyClosed || failed) return
      failed = true
      source.close()
      callbacks.onError(event)
    }
    return {
      close() {
        explicitlyClosed = true
        source.close()
      },
    }
  }

  url.protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const socket = new WebSocket(url.toString())
  socket.onopen = () => {
    if (!explicitlyClosed && !failed) callbacks.onOpen?.()
  }
  socket.onmessage = (event) => {
    if (!explicitlyClosed && !failed) callbacks.onMessage(String(event.data))
  }
  socket.onerror = (event) => {
    if (explicitlyClosed || failed) return
    failed = true
    socket.close()
    callbacks.onError(event)
  }
  socket.onclose = (event) => {
    if (explicitlyClosed || failed) return
    failed = true
    callbacks.onError(event as unknown as Event)
  }
  return {
    close() {
      explicitlyClosed = true
      socket.close()
    },
  }
}

export function parseRealtimeEnvelope(raw: string): RealtimeEnvelope | null {
  try {
    const value = JSON.parse(raw) as RealtimeEnvelope
    return value && typeof value.type === 'string' ? value : null
  } catch {
    return null
  }
}
