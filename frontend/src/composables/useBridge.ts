import { onMounted, onUnmounted, ref, shallowRef, computed } from 'vue'

import { openWebSocket, resolveApiBase, resolveWebSocketUrl } from '@/composables/backendEndpoints'
import type { BridgeMode, BridgeSnapshot } from '@/types/bridge'

/**
 * Kapselt Snapshot-Laden, Live-Updates und Moduswechsel fuer die Brueckenansicht.
 */
export function useBridge() {
  const bridgeMode = ref<BridgeMode>('AUTO')
  const submittingBridgeMode = ref<BridgeMode | null>(null)
  const snapshot = ref<BridgeSnapshot | null>(null)
  const loading = shallowRef(true)
  const error = shallowRef<string | null>(null)
  const websocket = shallowRef<WebSocket | null>(null)
  const websocketConnected = shallowRef(false)
  const reconnectTimer = shallowRef<number | null>(null)
  const manualClose = shallowRef(false)
  const apiBase = resolveApiBase()
  const webSocketUrl = resolveWebSocketUrl('/ws/bridge')

  const brokerConnected = computed(() => snapshot.value?.brokerConnected ?? false)
  const bridgeOnline = computed(() => snapshot.value?.espOnline ?? false)
  const liveConnected = computed(() => websocketConnected.value)

  /**
   * Holt den Initialzustand einmal per REST, bevor Live-Updates uebernehmen.
   */
  const loadSnapshot = async () => {
    loading.value = true
    error.value = null

    try {
      const response = await fetch(`${apiBase}/bridge`)
      if (!response.ok) {
        throw new Error(`Bridge snapshot request failed with status ${response.status}`)
      }

      snapshot.value = (await response.json()) as BridgeSnapshot
      bridgeMode.value = snapshot.value.mode
    } catch (requestError) {
      error.value = requestError instanceof Error ? requestError.message : 'Bridge snapshot request failed'
    } finally {
      loading.value = false
    }
  }

  /**
   * Sendet einen manuellen Moduswechsel an das Backend.
   */
  const setBridgeMode = async (mode: BridgeMode) => {
    submittingBridgeMode.value = mode
    error.value = null

    try {
      const response = await fetch(`${apiBase}/bridge/mode`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode }),
      })

      if (!response.ok) {
        throw new Error(`Bridge mode update failed with status ${response.status}`)
      }

      bridgeMode.value = mode
      await loadSnapshot()
    } catch (requestError) {
      error.value = requestError instanceof Error ? requestError.message : 'Bridge mode update failed'
    } finally {
      submittingBridgeMode.value = null
    }
  }

  /**
   * Verhindert hektische Reconnect-Loops bei kurzen Ausfaellen.
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
      snapshot.value = JSON.parse(event.data) as BridgeSnapshot
      bridgeMode.value = snapshot.value.mode
      error.value = null
    }

    nextSocket.onerror = () => {
      websocketConnected.value = false
      if (!snapshot.value) {
        error.value = 'Bridge WebSocket connection failed'
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

  return { bridgeMode, submittingBridgeMode, setBridgeMode, snapshot, loading, error, brokerConnected, bridgeOnline, liveConnected }
}
