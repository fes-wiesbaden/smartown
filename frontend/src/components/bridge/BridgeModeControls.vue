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
