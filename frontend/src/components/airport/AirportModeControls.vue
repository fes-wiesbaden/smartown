<script setup lang="ts">
import { computed } from 'vue'

import type { AirportMode } from '@/types/airport'

const props = defineProps<{
  currentMode: AirportMode
}>()

const emit = defineEmits<{
  setMode: [mode: AirportMode]
}>()

const modes: Array<{ value: AirportMode; label: string; description: string }> = [
  { value: 'ON', label: 'An', description: 'Lichter aktiv, Flugzeugerkennung und Messung laufen mit.' },
  { value: 'OFF', label: 'Aus', description: 'Lichter aus, Flugzeugerkennung und Messung sind deaktiviert.' },
]

const currentModeLabel = computed(() => (props.currentMode === 'ON' ? 'An' : 'Aus'))
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
        @click="emit('setMode', mode.value)"
      >
        <span class="controls__button-label">{{ mode.label }}</span>
        <span class="controls__button-description">{{ mode.description }}</span>
      </button>
      <div class="controls__button-placeholder" aria-hidden="true"></div>
    </div>
  </section>
</template>

<style scoped>
.controls {
  display: grid;
  height: 100%;
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

.controls__button {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 8px;
  width: 100%;
  height: 184px;
  border: 1px solid var(--theme-card-border);
  border-radius: 10px;
  padding: 16px;
  color: #172026;
  background: var(--theme-surface);
  text-align: left;
  cursor: pointer;
}

.controls__button--active {
  border-color: var(--theme-accent);
  background: var(--theme-accent-soft);
}

.controls__button-label {
  font-size: 1rem;
  font-weight: 800;
}

.controls__button-description {
  color: var(--theme-muted);
  font-size: 0.875rem;
  line-height: 1.4;
  white-space: normal;
  overflow-wrap: anywhere;
  hyphens: auto;
}

.controls__button-placeholder {
  visibility: hidden;
}

@media (max-width: 840px) {
  .controls__buttons {
    grid-template-columns: 1fr;
  }

  .controls__button-placeholder {
    display: none;
  }
}
</style>
