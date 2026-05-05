import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue'

import { openWebSocket, resolveApiBase, resolveWebSocketUrl } from '@/composables/backendEndpoints'
import type { AirportMode, AirportSnapshot } from '@/types/airport'

export function useAirport() {
  const snapshot = ref<AirportSnapshot | null>(null)
  const loading = shallowRef(true)
  const error = shallowRef<string | null>(null)
  const submittingMode = shallowRef<AirportMode | null>(null)
  const pendingMode = shallowRef<AirportMode | null>(null)
  const latestModeRequestId = shallowRef(0)
  const websocket = shallowRef<WebSocket | null>(null)
  const websocketConnected = shallowRef(false)
  const reconnectTimer = shallowRef<number | null>(null)
  const manualClose = shallowRef(false)

  const apiBase = resolveApiBase()
  const webSocketUrl = resolveWebSocketUrl('/ws/airport')

  const brokerConnected = computed(() => snapshot.value?.brokerConnected ?? false)
  const airportOnline = computed(() => snapshot.value?.state.online ?? false)
  const liveConnected = computed(() => websocketConnected.value)

  async function loadSnapshot() {
    loading.value = true
    error.value = null

    try {
      const response = await fetch(`${apiBase}/airport`)
      if (!response.ok) {
        throw new Error(`Airport snapshot request failed with status ${response.status}`)
      }

      snapshot.value = (await response.json()) as AirportSnapshot
    } catch (requestError) {
      error.value = requestError instanceof Error ? requestError.message : 'Airport snapshot request failed'
    } finally {
      loading.value = false
    }
  }

  async function setMode(mode: AirportMode) {
    const requestId = latestModeRequestId.value + 1
    latestModeRequestId.value = requestId
    submittingMode.value = mode
    pendingMode.value = mode
    error.value = null

    try {
      const response = await fetch(`${apiBase}/airport/mode`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ mode }),
      })

      if (!response.ok) {
        throw new Error(`Airport mode update failed with status ${response.status}`)
      }

      if (requestId !== latestModeRequestId.value) {
        return
      }

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
      if (requestId === latestModeRequestId.value) {
        pendingMode.value = null
        error.value = requestError instanceof Error ? requestError.message : 'Airport mode update failed'
      }
    } finally {
      if (requestId === latestModeRequestId.value) {
        submittingMode.value = null
      }
    }
  }

  function scheduleReconnect() {
    if (manualClose.value || reconnectTimer.value !== null) {
      return
    }

    reconnectTimer.value = window.setTimeout(() => {
      reconnectTimer.value = null
      connectWebSocket()
    }, 3000)
  }

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
      const nextSnapshot = JSON.parse(event.data) as AirportSnapshot

      if (pendingMode.value !== null && nextSnapshot.state.mode !== pendingMode.value) {
        snapshot.value = {
          ...nextSnapshot,
          state: {
            ...nextSnapshot.state,
            mode: pendingMode.value,
          },
        }
      } else {
        pendingMode.value = null
        snapshot.value = nextSnapshot
      }

      error.value = null
    }

    nextSocket.onerror = () => {
      websocketConnected.value = false
      if (!snapshot.value) {
        error.value = 'Airport WebSocket connection failed'
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
    airportOnline,
    brokerConnected,
    error,
    liveConnected,
    loading,
    setMode,
    snapshot,
    submittingMode,
  }
}
