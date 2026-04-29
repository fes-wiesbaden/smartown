import { ref } from 'vue'
import type { BridgeMode } from '@/types/bridge'

export function useBridge() {
  const bridgeMode = ref<BridgeMode>('AUTO')
  const submittingBridgeMode = ref<BridgeMode | null>(null)

  const setBridgeMode = async (mode: BridgeMode) => {
    submittingBridgeMode.value = mode
    try {
      await fetch('/api/bridge/mode', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode }),
      })
      bridgeMode.value = mode
    } catch (e) {
      console.error(e)
    } finally {
      submittingBridgeMode.value = null
    }
  }

  return { bridgeMode, submittingBridgeMode, setBridgeMode }
}
