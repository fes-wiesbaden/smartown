<script setup lang="ts">
import { computed } from 'vue'

import type { BridgeMode } from '@/types/bridge'

const props = defineProps<{
  controlsEnabled: boolean
  currentMode: BridgeMode | null
  submittingMode: BridgeMode | null
}>()

const emit = defineEmits<{
  setMode: [mode: BridgeMode]
}>()

const modes: Array<{ value: BridgeMode; label: string; description: string }> = [
  { value: 'AUTO', label: 'Auto', description: 'Sensoren steuern die Brücke automatisch.' },
  { value: 'MANUAL_OPEN', label: 'Hoch', description: 'Brücke wird sicher hochgefahren (max. 1x).' },
  { value: 'MANUAL_CLOSE', label: 'Runter', description: 'Brücke wird sicher runtergefahren (max. 1x).' },
]

const currentModeLabel = computed(() => {
  if (props.currentMode === null) {
    return 'unbekannt'
  }
  if (props.currentMode === 'AUTO') {
    return 'Auto'
  }

  return props.currentMode === 'MANUAL_OPEN' ? 'Hoch' : 'Runter'
})

const requestPending = computed(() => props.submittingMode !== null)
const controlsBlocked = computed(() => !props.controlsEnabled)
const buttonsDisabled = computed(() => requestPending.value || controlsBlocked.value)
</script>

<template>
  <section class="controls" aria-labelledby="bridge-controls-title">
    <div class="controls__header">
      <div>
        <p class="controls__eyebrow">Steuerung</p>
        <h2 id="bridge-controls-title" class="controls__title">Brückenmodus</h2>
      </div>
      <span class="controls__mode">{{ currentModeLabel }}</span>
    </div>

    <p v-if="!controlsEnabled" class="controls__notice">
      Steuerung erst moeglich, wenn Broker und ESP32 online sind.
    </p>

    <div class="controls__buttons">
      <button
        v-for="mode in modes"
        :key="mode.value"
        class="controls__button"
        :class="{
          'controls__button--active': currentMode === mode.value,
          'controls__button--blocked': controlsBlocked,
          'controls__button--busy': requestPending,
          'controls__button--pending': submittingMode === mode.value,
        }"
        type="button"
        :disabled="buttonsDisabled"
        @click="emit('setMode', mode.value)"
      >
        <span class="controls__button-label">{{ mode.label }}</span>
        <span class="controls__button-description">{{ mode.description }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.controls {
  display: grid;
  gap: 20px;
  border: 1px solid var(--theme-card-border);
  border-radius: 14px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 40px rgba(96, 53, 250, 0.08);
}

.controls__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.controls__eyebrow {
  margin: 0 0 4px;
  color: var(--theme-accent);
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.controls__title {
  margin: 0;
  color: #172026;
  font-size: 1.125rem;
  font-weight: 800;
}

.controls__mode {
  border: 1px solid #d8dfe2;
  border-radius: 999px;
  padding: 6px 10px;
  color: var(--theme-accent-strong);
  background: var(--theme-accent-soft);
  font-size: 0.8125rem;
  font-weight: 700;
}

.controls__buttons {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.controls__notice {
  margin: 0;
  color: var(--theme-offline);
  font-size: 0.875rem;
  font-weight: 600;
}

.controls__button {
  display: grid;
  gap: 8px;
  min-height: 124px;
  border: 1px solid var(--theme-card-border);
  border-radius: 10px;
  padding: 16px;
  color: #172026;
  background: var(--theme-surface);
  text-align: left;
  cursor: pointer;
}

.controls__button:disabled {
  opacity: 0.75;
}

.controls__button--blocked:disabled {
  cursor: not-allowed;
}

.controls__button--busy:disabled {
  cursor: wait;
}

.controls__button--active {
  border-color: var(--theme-accent);
  background: var(--theme-accent-soft);
}

.controls__button--pending {
  border-color: #6a7d86;
  background: #edf1f3;
}

.controls__button-label {
  font-size: 1rem;
  font-weight: 800;
}

.controls__button-description {
  color: var(--theme-muted);
  font-size: 0.875rem;
  line-height: 1.4;
}

@media (max-width: 840px) {
  .controls__buttons {
    grid-template-columns: 1fr;
  }
}
</style>
