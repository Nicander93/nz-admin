import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getRealtimeStats,
  getRealtimeTicket,
  sendRealtimeTest,
  type RealtimeConnectionStats,
  type RealtimeEnvelope,
  type RealtimeTransport,
} from '@/api/system/realtime'

export type RealtimeStatus = 'disconnected' | 'connecting' | 'connected' | 'error'

export function useRealtimeConsole() {
  const transport = ref<RealtimeTransport>('SSE')
  const status = ref<RealtimeStatus>('disconnected')
  const events = ref<RealtimeEnvelope[]>([])
  const stats = ref<RealtimeConnectionStats>({
    sseConnections: 0,
    webSocketConnections: 0,
    totalConnections: 0,
  })
  const testMessage = ref('nz-admin 实时消息测试')
  let connection: EventSource | WebSocket | null = null

  const connected = computed(() => status.value === 'connected')

  function appendEvent(raw: string) {
    try {
      const event = JSON.parse(raw) as RealtimeEnvelope
      events.value.unshift(event)
      events.value = events.value.slice(0, 100)
    } catch {
      events.value.unshift({
        id: crypto.randomUUID(),
        type: 'unknown',
        sentAt: new Date().toISOString(),
        payload: raw,
      })
    }
  }

  function disconnect() {
    connection?.close()
    connection = null
    status.value = 'disconnected'
  }

  async function connect() {
    disconnect()
    status.value = 'connecting'
    try {
      const response = await getRealtimeTicket(transport.value)
      const url = new URL(response.data.path, window.location.origin)
      url.searchParams.set('ticket', response.data.ticket)

      if (transport.value === 'SSE') {
        const source = new EventSource(url.toString())
        source.onopen = () => { status.value = 'connected' }
        source.onmessage = (event) => appendEvent(event.data)
        source.onerror = () => {
          source.close()
          if (connection === source) {
            connection = null
            status.value = 'error'
          }
        }
        connection = source
        return
      }

      url.protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const socket = new WebSocket(url.toString())
      socket.onopen = () => { status.value = 'connected' }
      socket.onmessage = (event) => appendEvent(String(event.data))
      socket.onerror = () => { status.value = 'error' }
      socket.onclose = () => {
        if (connection === socket) {
          connection = null
          status.value = 'disconnected'
        }
      }
      connection = socket
    } catch (error) {
      status.value = 'error'
      throw error
    }
  }

  async function refreshStats() {
    const response = await getRealtimeStats()
    stats.value = response.data
  }

  async function sendTest() {
    const response = await sendRealtimeTest(testMessage.value)
    ElMessage.success(`消息已投递到 ${response.data} 个连接`)
    await refreshStats()
  }

  function clearEvents() {
    events.value = []
  }

  return {
    transport,
    status,
    connected,
    events,
    stats,
    testMessage,
    connect,
    disconnect,
    refreshStats,
    sendTest,
    clearEvents,
  }
}
