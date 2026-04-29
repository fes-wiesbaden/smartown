import { onMounted, onUnmounted, ref, shallowRef, computed } from 'vue'
import type { BridgeMode, BridgeSnapshot } from '@/types/bridge'

const DEV_SERVER_PORT = '5173'
const BACKEND_PORT = '8080'

function resolveApiBase(): string {
  if (window.location.port === DEV_SERVER_PORT) {
    return `${window.location.protocol}//${window.location.hostname}:${BACKEND_PORT}/api`
  }
  return '/api'
}

export function useBridge() {
  const bridgeMode = ref<BridgeMode>('AUTO')
  const submittingBridgeMode = ref<BridgeMode | null>(null)
  const snapshot = ref<BridgeSnapshot | null>(null)
  const loading = shallowRef(true)
  const error = shallowRef<string | null>(null)
  const apiBase = resolveApiBase()

  const brokerConnected = computed(() => snapshot.value?.brokerConnected ?? false)
  const bridgeOnline = computed(() => snapshot.value?.espOnline ?? false)
  let pollInterval: number | null = null

  const loadSnapshot = async () => {
    try {
      const response = await fetch(`${apiBase}/bridge`)
      if (response.ok) {
        snapshot.value = await response.json()
        bridgeMode.value = snapshot.value!.mode
        error.value = null
      }
    } catch (e) {
      error.value = 'Fehler beim Laden des Brücken-Status'
    } finally {
      loading.value = false
    }
  }

  const setBridgeMode = async (mode: BridgeMode) => {
    submittingBridgeMode.value = mode
    try {
      await fetch(`${apiBase}/bridge/mode`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode }),
      })
      bridgeMode.value = mode
      await loadSnapshot()
    } catch (e) {
      console.error(e)
    } finally {
      submittingBridgeMode.value = null
    }
  }

  onMounted(() => {
    loadSnapshot()
    // Pollen alle 3 Sekunden, da wir fuer den MVP noch keine WebSockets für die Bruecke haben
    pollInterval = window.setInterval(loadSnapshot, 3000)
  })

  onUnmounted(() => {
    if (pollInterval) clearInterval(pollInterval)
  })

  return { bridgeMode, submittingBridgeMode, setBridgeMode, snapshot, loading, error, brokerConnected, bridgeOnline }
}
