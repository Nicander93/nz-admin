import request from '@/api/request'

export type RealtimeTransport = 'SSE' | 'WEBSOCKET'

export interface RealtimeTicket {
  ticket: string
  transport: RealtimeTransport
  path: string
  expiresInSeconds: number
}

export interface RealtimeConnectionStats {
  sseConnections: number
  webSocketConnections: number
  totalConnections: number
}

export interface RealtimeEnvelope {
  id: string
  type: string
  sentAt: string
  payload: unknown
}

export function getRealtimeTicket(transport: RealtimeTransport) {
  return request.get<RealtimeTicket>('/api/system/realtime/ticket', {
    params: { transport },
  })
}

export function getRealtimeStats() {
  return request.get<RealtimeConnectionStats>('/api/system/realtime/stats')
}

export function sendRealtimeTest(message: string) {
  return request.post<number>('/api/system/realtime/test', { message })
}
