import { defineStore } from 'pinia'
import { ref } from 'vue'

import { getUnreadMessageCount } from '@/api/system/message'
import { getNotificationRealtimeTicket } from '@/api/system/realtime'
import { createRealtimeConnection, parseRealtimeEnvelope } from '@/core/realtime/client'
import type { RealtimeConnectionHandle } from '@/core/realtime/client'

const ALLOWED_EVENTS = new Set(['system-message', 'system-message-state'])
const POLL_INTERVAL = 60_000
const MAX_RETRY_DELAY = 30_000

export const useMessageRealtimeStore = defineStore('message-realtime', () => {
  const unreadCount = ref(0)
  const revision = ref(0)
  const activeIdentity = ref('')
  let generation = 0
  let connectionAttempt = 0
  let connection: RealtimeConnectionHandle | undefined
  let pollTimer: ReturnType<typeof setInterval> | undefined
  let retryTimer: ReturnType<typeof setTimeout> | undefined
  let retryAttempt = 0
  let ticketController: AbortController | undefined
  let countController: AbortController | undefined
  let countSequence = 0
  let connecting = false
  let networkOffline = false
  let removeNetworkListeners: (() => void) | undefined

  function notifyIdentity() {
    window.dispatchEvent(new CustomEvent('message-identity-changed', { detail: { identity: activeIdentity.value } }))
  }

  function notifyInbox() {
    revision.value += 1
    window.dispatchEvent(new CustomEvent('message-inbox-changed'))
  }

  function isActive(expectedGeneration: number, identity: string) {
    return expectedGeneration === generation && identity !== '' && activeIdentity.value === identity
  }

  function isCurrentAttempt(expectedGeneration: number, identity: string, attempt: number) {
    return isActive(expectedGeneration, identity) && attempt === connectionAttempt && !networkOffline
  }

  async function refreshCount(expectedGeneration = generation) {
    if (!activeIdentity.value || expectedGeneration !== generation || networkOffline) return
    countController?.abort()
    const controller = new AbortController()
    countController = controller
    const sequence = ++countSequence
    const identity = activeIdentity.value
    try {
      const response = await getUnreadMessageCount({ signal: controller.signal, silentError: true })
      if (!controller.signal.aborted && expectedGeneration === generation && identity === activeIdentity.value && sequence === countSequence && !networkOffline) {
        unreadCount.value = response.data ?? 0
      }
    } catch {
      // Background count failures stay quiet; polling or reconnect will retry.
    } finally {
      if (countController === controller) countController = undefined
    }
  }

  function closeConnection() {
    connection?.close()
    connection = undefined
  }

  function clearRetryTimer() {
    if (retryTimer) clearTimeout(retryTimer)
    retryTimer = undefined
  }

  function stopPolling() {
    if (pollTimer) clearInterval(pollTimer)
    pollTimer = undefined
  }

  function startPolling(expectedGeneration: number) {
    stopPolling()
    if (networkOffline || expectedGeneration !== generation || !activeIdentity.value) return
    pollTimer = setInterval(() => {
      if (networkOffline || expectedGeneration !== generation) return
      void refreshCount(expectedGeneration)
      notifyInbox()
    }, POLL_INTERVAL)
  }

  function scheduleReconnect(expectedGeneration: number, identity: string, attempt: number) {
    if (!isCurrentAttempt(expectedGeneration, identity, attempt)) return
    connectionAttempt += 1
    closeConnection()
    if (retryTimer) return
    const delay = Math.min(1000 * 2 ** retryAttempt, MAX_RETRY_DELAY)
    retryAttempt += 1
    retryTimer = setTimeout(() => {
      retryTimer = undefined
      void connect(expectedGeneration, identity)
    }, delay)
  }

  async function connect(expectedGeneration: number, identity: string) {
    if (connecting || !isActive(expectedGeneration, identity) || networkOffline) return
    connecting = true
    const attempt = ++connectionAttempt
    const controller = new AbortController()
    ticketController?.abort()
    ticketController = controller
    try {
      const response = await getNotificationRealtimeTicket({ signal: controller.signal, silentError: true })
      if (controller.signal.aborted || !isCurrentAttempt(expectedGeneration, identity, attempt)) return
      const handle = createRealtimeConnection(response.data, 'SSE', {
        onOpen: () => {
          if (!isCurrentAttempt(expectedGeneration, identity, attempt)) return
          retryAttempt = 0
          void refreshCount(expectedGeneration)
          notifyInbox()
        },
        onMessage: (raw) => {
          if (!isCurrentAttempt(expectedGeneration, identity, attempt)) return
          const event = parseRealtimeEnvelope(raw)
          if (!event || !ALLOWED_EVENTS.has(event.type)) return
          void refreshCount(expectedGeneration)
          notifyInbox()
        },
        onError: () => {
          if (!isCurrentAttempt(expectedGeneration, identity, attempt)) return
          scheduleReconnect(expectedGeneration, identity, attempt)
        },
      })
      if (!isCurrentAttempt(expectedGeneration, identity, attempt)) handle.close()
      else connection = handle
    } catch {
      if (!controller.signal.aborted && isCurrentAttempt(expectedGeneration, identity, attempt)) {
        scheduleReconnect(expectedGeneration, identity, attempt)
      }
    } finally {
      if (ticketController === controller) {
        ticketController = undefined
        connecting = false
      }
    }
  }

  function installNetworkListeners(expectedGeneration: number, identity: string) {
    const onOffline = () => {
      if (!isActive(expectedGeneration, identity) || networkOffline) return
      networkOffline = true
      connectionAttempt += 1
      ticketController?.abort()
      ticketController = undefined
      connecting = false
      countController?.abort()
      countController = undefined
      countSequence += 1
      clearRetryTimer()
      stopPolling()
      closeConnection()
    }
    const onOnline = () => {
      if (!isActive(expectedGeneration, identity) || !networkOffline) return
      networkOffline = false
      retryAttempt = 0
      startPolling(expectedGeneration)
      void refreshCount(expectedGeneration)
      notifyInbox()
      void connect(expectedGeneration, identity)
    }
    window.addEventListener('offline', onOffline)
    window.addEventListener('online', onOnline)
    removeNetworkListeners = () => {
      window.removeEventListener('offline', onOffline)
      window.removeEventListener('online', onOnline)
    }
  }

  function start(identity: string) {
    if (!identity) return stop()
    if (activeIdentity.value === identity && (connection || connecting || retryTimer)) return
    stop()
    activeIdentity.value = identity
    networkOffline = false
    notifyIdentity()
    const current = generation
    installNetworkListeners(current, identity)
    if (typeof navigator !== 'undefined' && navigator.onLine === false) {
      networkOffline = true
      return
    }
    void refreshCount(current)
    notifyInbox()
    startPolling(current)
    void connect(current, identity)
  }

  function stop() {
    generation += 1
    connectionAttempt += 1
    activeIdentity.value = ''
    networkOffline = false
    notifyIdentity()
    removeNetworkListeners?.()
    removeNetworkListeners = undefined
    ticketController?.abort()
    ticketController = undefined
    countController?.abort()
    countController = undefined
    countSequence += 1
    closeConnection()
    stopPolling()
    clearRetryTimer()
    retryAttempt = 0
    connecting = false
    unreadCount.value = 0
  }

  function refreshFromLocalMutation() {
    void refreshCount()
    notifyInbox()
  }

  return { unreadCount, revision, activeIdentity, start, stop, refreshCount, refreshFromLocalMutation }
})
