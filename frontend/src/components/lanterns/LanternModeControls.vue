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
