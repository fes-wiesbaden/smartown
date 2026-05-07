import { computed, onMounted, onUnmounted, readonly, ref, shallowRef } from 'vue'

import { resolveApiBase } from '@/composables/backendEndpoints'
import type { LanternLuxHistoryPoint } from '@/types/lanterns'

const HISTORY_REFRESH_INTERVAL_MS = 60_000

/**
 * Laedt die persistierte Lux-Historie aus der Datenbank fuer die Anzeige im Verlauf.
 */
export function useLanternLuxHistory() {
  const persistedHistory = ref<LanternLuxHistoryPoint[]>([])
  const loading = shallowRef(true)
  const error = shallowRef<string | null>(null)
  const refreshTimer = shallowRef<number | null>(null)
  const apiBase = resolveApiBase()

  async function loadHistory() {
    try {
      error.value = null
      const response = await fetch(`${apiBase}/lanterns/history`)
      if (!response.ok) {
        throw new Error(`Lux history request failed with status ${response.status}`)
      }

      const history = await response.json()
      if (!Array.isArray(history)) {
        throw new Error('Lux history response invalid')
      }

      persistedHistory.value = history as LanternLuxHistoryPoint[]
    } catch (requestError) {
      error.value = requestError instanceof Error ? requestError.message : 'Lux history request failed'
    } finally {
      loading.value = false
    }
  }

  const points = computed(() => persistedHistory.value)

  onMounted(async () => {
    loading.value = true
    await loadHistory()
    refreshTimer.value = window.setInterval(() => {
      void loadHistory()
    }, HISTORY_REFRESH_INTERVAL_MS)
  })

  onUnmounted(() => {
    if (refreshTimer.value !== null) {
      window.clearInterval(refreshTimer.value)
      refreshTimer.value = null
    }
  })

  return {
    error: readonly(error),
    loading: readonly(loading),
    points,
    reload: loadHistory,
  }
}
