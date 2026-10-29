import { ElMessage } from 'element-plus'
import { computed, getCurrentScope, onScopeDispose, ref, watch } from 'vue'

import {
  getRealtimeStats,
  getRealtimeTicket,
  sendRealtimeTest,
  type RealtimeConnectionStats,
  type RealtimeEnvelope,
  type RealtimeTransport,
} from '@/api/system/realtime'
import { createRealtimeConnection, parseRealtimeEnvelope, type RealtimeConnectionHandle } from '@/core/realtime/client'

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
  let connection: RealtimeConnectionHandle | null = null
  let connectionAttempt = 0
  let connectionController: AbortController | null = null

  const connected = computed(() => status.value === 'connected')

  function isCurrentAttempt(attempt: number, controller: AbortController, selectedTransport: RealtimeTransport) {
    return connectionAttempt === attempt
      && connectionController === controller
      && !controller.signal.aborted
      && transport.value === selectedTransport
  }

  function appendEvent(raw: string) {
    const event = parseRealtimeEnvelope(raw)
    if (event) {
      events.value.unshift(event)
    } else {
      events.value.unshift({
        id: crypto.randomUUID(),
        type: 'unknown',
        sentAt: new Date().toISOString(),
        payload: raw,
      })
    }
    events.value = events.value.slice(0, 100)
  }

  function disconnect() {
    connectionAttempt += 1
    connectionController?.abort()
    connectionController = null
    connection?.close()
    connection = null
    status.value = 'disconnected'
  }

  async function connect() {
    disconnect()
    const attempt = ++connectionAttempt
    const controller = new AbortController()
    const selectedTransport = transport.value
    connectionController = controller
    status.value = 'connecting'

    try {
      const response = await getRealtimeTicket(selectedTransport, { signal: controller.signal })
      if (!isCurrentAttempt(attempt, controller, selectedTransport)) return

      const handle = createRealtimeConnection(response.data, selectedTransport, {
        onOpen: () => {
          if (isCurrentAttempt(attempt, controller, selectedTransport)) status.value = 'connected'
        },
        onMessage: (raw) => {
          if (isCurrentAttempt(attempt, controller, selectedTransport)) appendEvent(raw)
        },
        onError: () => {
          if (!isCurrentAttempt(attempt, controller, selectedTransport)) return
          if (connection === handle) connection = null
          connectionAttempt += 1
          controller.abort()
          connectionController = null
          status.value = 'error'
        },
      })

      if (!isCurrentAttempt(attempt, controller, selectedTransport)) {
        handle.close()
        return
      }
      connection = handle
    } catch (error) {
      if (!isCurrentAttempt(attempt, controller, selectedTransport)) return
      connectionAttempt += 1
      controller.abort()
      connectionController = null
      status.value = 'error'
      throw error
    }
  }

  if (getCurrentScope()) {
    watch(transport, disconnect)
    onScopeDispose(disconnect)
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
