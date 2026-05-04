<script setup lang="ts">
import { computed } from 'vue'

import type { LanternSnapshot } from '@/types/lanterns'

/**
 * Enthalten den letzten Snapshot plus Lade- und Fehlerzustand fuer die Statuskarte.
 */
const props = defineProps<{
  error: string | null
  loading: boolean
  snapshot: LanternSnapshot | null
}>()

/**
 * Macht den technischen Modus im UI lesbar.
 */
const modeLabel = computed(() => {
  const mode = props.snapshot?.state.mode
  if (!mode) {
    return '-'
  }

  if (mode === 'AUTO') {
    return 'Auto'
  }

  return mode === 'ON' ? 'An' : 'Aus'
})

/**
 * Uebersetzt den technischen Lichtzustand in eine lesbare Anzeige.
 */
const lightLabel = computed(() => {
  if (!props.snapshot) {
    return '-'
  }

  return props.snapshot.state.lightState === 'ON' ? 'An' : 'Aus'
})

/**
 * Formatiert den aktuell gemeldeten Schwellwert mit Einheit.
 */
const thresholdLabel = computed(() => {
  const threshold = props.snapshot?.state.thresholdLux
  return threshold === null || threshold === undefined ? '-' : `${threshold.toFixed(1)} lx`
})

const lanternConnectionLabel = computed(() => (props.snapshot?.state.online ? 'Online' : 'Offline'))
</script>

<template>
  <section class="status-card" aria-labelledby="lantern-status-title">
    <div class="status-card__header">
      <div>
        <p class="status-card__eyebrow">Laternen</p>
        <h2 id="lantern-status-title" class="status-card__title">Laternenstatus</h2>
      </div>
      <p class="status-card__connection">
        <span
          class="status-card__connection-dot"
          :class="snapshot?.state.online ? 'status-card__connection-dot--online' : 'status-card__connection-dot--offline'"
          aria-hidden="true"
        ></span>
        {{ lanternConnectionLabel }}
      </p>
    </div>

    <p v-if="loading" class="status-card__notice">Snapshot wird geladen.</p>
    <p v-else-if="error" class="status-card__notice status-card__notice--error">{{ error }}</p>

    <div v-if="snapshot" class="status-card__grid">
      <article class="status-card__item">
        <span class="status-card__label">Modus</span>
        <strong class="status-card__value">{{ modeLabel }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Licht</span>
        <strong class="status-card__value">{{ lightLabel }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Schwellwert</span>
        <strong class="status-card__value">{{ thresholdLabel }}</strong>
      </article>
    </div>
  </section>
</template>
