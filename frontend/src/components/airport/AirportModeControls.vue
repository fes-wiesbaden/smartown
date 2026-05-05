<script setup lang="ts">
import { computed } from 'vue'

import type { AirportMode } from '@/types/airport'

const props = defineProps<{
  controlsEnabled: boolean
  currentMode: AirportMode | null
  submittingMode: AirportMode | null
}>()

const emit = defineEmits<{
  setMode: [mode: AirportMode]
}>()

const modes: Array<{ value: AirportMode; label: string; description: string }> = [
  { value: 'ON', label: 'An', description: 'Lichter an, Erkennung aktiv.' },
  { value: 'OFF', label: 'Aus', description: 'Lichter aus, Erkennung pausiert.' },
]

const currentModeLabel = computed(() => {
  if (props.currentMode === 'ON') {
    return 'An'
  }
  if (props.currentMode === 'OFF') {
    return 'Aus'
  }

  return 'Unbekannt'
})
</script>

<template>
  <section class="controls" aria-labelledby="airport-controls-title">
    <div class="controls__header">
      <div>
        <p class="controls__eyebrow">Steuerung</p>
        <h2 id="airport-controls-title" class="controls__title">Flughafenmodus</h2>
      </div>
      <span class="controls__mode">{{ currentModeLabel }}</span>
    </div>

    <div class="controls__buttons">
      <button
        v-for="mode in modes"
        :key="mode.value"
        class="controls__button"
        :class="{ 'controls__button--active': currentMode === mode.value }"
        type="button"
        :disabled="!controlsEnabled || submittingMode !== null"
        @click="emit('setMode', mode.value)"
      >
        <span class="controls__button-label">{{ mode.label }}</span>
        <span class="controls__button-description">{{ mode.description }}</span>
      </button>
      <div class="controls__button-placeholder" aria-hidden="true"></div>
    </div>
  </section>
</template>
