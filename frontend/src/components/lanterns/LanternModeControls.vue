<script setup lang="ts">
import { computed } from 'vue'

import type { LanternMode } from '@/types/lanterns'

/**
 * Props fuer den aktuell sichtbaren Modus und laufende REST-Aktionen.
 */
const props = defineProps<{
  controlsEnabled: boolean
  currentMode: LanternMode | null
  submittingMode: LanternMode | null
}>()

/**
 * Liefert den gewaehlten Modus an den uebergeordneten Container zurueck.
 */
const emit = defineEmits<{
  setMode: [mode: LanternMode]
}>()

/**
 * Beschreibt die drei unterstuetzten Bedienmodi fuer das Frontend.
 */
const modes: Array<{ value: LanternMode; label: string; description: string }> = [
  { value: 'AUTO', label: 'Auto', description: 'BH1750 steuert die Laternen automatisch.' },
  { value: 'ON', label: 'Ein', description: 'Laternen bleiben manuell eingeschaltet.' },
  { value: 'OFF', label: 'Aus', description: 'Laternen bleiben manuell ausgeschaltet.' },
]

/**
 * Formatiert den aktiven Modus fuer das Kopf-Badge lesbar.
 */
const currentModeLabel = computed(() => {
  if (props.currentMode === null) {
    return 'unbekannt'
  }
  if (props.currentMode === 'AUTO') {
    return 'Auto'
  }

  return props.currentMode === 'ON' ? 'An' : 'Aus'
})

/**
 * Sperrt die Steuerung bei ausstehendem Request oder fehlender Broker-/ESP32-Verbindung.
 */
const requestPending = computed(() => props.submittingMode !== null)
const controlsBlocked = computed(() => !props.controlsEnabled)
const buttonsDisabled = computed(() => requestPending.value || controlsBlocked.value)
</script>

<template>
  <section class="controls" aria-labelledby="lantern-controls-title">
    <div class="controls__header">
      <div>
        <p class="controls__eyebrow">Steuerung</p>
        <h2 id="lantern-controls-title" class="controls__title">Laternenmodus</h2>
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
  white-space: normal;
  overflow-wrap: anywhere;
  hyphens: auto;
}

@media (max-width: 840px) {
  .controls__buttons {
    grid-template-columns: 1fr;
  }
}
</style>
