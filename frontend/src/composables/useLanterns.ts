import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue'

import { openWebSocket, resolveApiBase, resolveWebSocketUrl } from '@/composables/backendEndpoints'
import type { LanternMode, LanternSnapshot } from '@/types/lanterns'

/**
 * Kapselt Snapshot-Laden, Live-Updates und Moduswechsel fuer die Laternenansicht.
 */
export function useLanterns() {
  const snapshot = ref<LanternSnapshot | null>(null)
  const loading = shallowRef(true)
  const error = shallowRef<string | null>(null)
  const submittingMode = shallowRef<LanternMode | null>(null)
  const websocket = shallowRef<WebSocket | null>(null)
  const websocketConnected = shallowRef(false)
  const reconnectTimer = shallowRef<number | null>(null)
  const manualClose = shallowRef(false)

  const apiBase = resolveApiBase()
  const webSocketUrl = resolveWebSocketUrl('/ws/lanterns')

  const brokerConnected = computed(() => snapshot.value?.brokerConnected ?? false)
  const lanternOnline = computed(() => snapshot.value?.state.online ?? false)
  const liveConnected = computed(() => websocketConnected.value)

  /**
   * Laedt den zuletzt bekannten Snapshot einmal per REST.
   */
  async function loadSnapshot() {
    loading.value = true
    error.value = null

    try {
      const response = await fetch(`${apiBase}/lanterns`)
      if (!response.ok) {
        throw new Error(`Snapshot request failed with status ${response.status}`)
      }

      snapshot.value = (await response.json()) as LanternSnapshot
    } catch (requestError) {
      error.value = requestError instanceof Error ? requestError.message : 'Snapshot request failed'
    } finally {
      loading.value = false
    }
  }

  /**
   * Sendet einen manuellen Moduswechsel an das Backend.
   */
  async function setMode(mode: LanternMode) {
    submittingMode.value = mode
    error.value = null

    try {
      const response = await fetch(`${apiBase}/lanterns/mode`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ mode }),
      })

      if (!response.ok) {
        throw new Error(`Mode update failed with status ${response.status}`)
      }

      // Das Backend bestaetigt den Command sofort, der echte Zielzustand kommt aber asynchron per WebSocket.
      // Deshalb darf ein alter REST-Snapshot den gerade geklickten Modus nicht wieder auf AUTO zuruecksetzen.
      if (snapshot.value) {
        snapshot.value = {
          ...snapshot.value,
          state: {
            ...snapshot.value.state,
            mode,
          },
        }
      }
    } catch (requestError) {
      error.value = requestError instanceof Error ? requestError.message : 'Mode update failed'
    } finally {
      submittingMode.value = null
    }
  }

  /**
   * Plant einen spaeteren Reconnect, damit Frontend und Backend bei kurzen Ausfaellen wieder zusammenfinden.
   */
  function scheduleReconnect() {
    if (manualClose.value || reconnectTimer.value !== null) {
      return
    }

    reconnectTimer.value = window.setTimeout(() => {
      reconnectTimer.value = null
      connectWebSocket()
    }, 3000)
  }

  /**
   * Baut genau eine aktive WebSocket-Verbindung fuer Live-Snapshots auf.
   */
  function connectWebSocket() {
    if (websocket.value?.readyState === WebSocket.OPEN || websocket.value?.readyState === WebSocket.CONNECTING) {
      return
    }

    const nextSocket = openWebSocket(webSocketUrl)
    websocket.value = nextSocket

    nextSocket.onopen = () => {
      websocketConnected.value = true
    }

    nextSocket.onmessage = (event) => {
      snapshot.value = JSON.parse(event.data) as LanternSnapshot
      error.value = null
    }

    nextSocket.onerror = () => {
      websocketConnected.value = false
      if (!snapshot.value) {
        error.value = 'WebSocket connection failed'
      }
    }

    nextSocket.onclose = () => {
      websocketConnected.value = false
      websocket.value = null
      scheduleReconnect()
    }
  }

  onMounted(async () => {
    manualClose.value = false
    await loadSnapshot()
    connectWebSocket()
  })

  onUnmounted(() => {
    manualClose.value = true
    if (reconnectTimer.value !== null) {
      window.clearTimeout(reconnectTimer.value)
      reconnectTimer.value = null
    }
    websocketConnected.value = false
    websocket.value?.close()
    websocket.value = null
  })

  return {
    brokerConnected,
    error,
    liveConnected,
    lanternOnline,
    loading,
    setMode,
    snapshot,
    submittingMode,
  }
}
