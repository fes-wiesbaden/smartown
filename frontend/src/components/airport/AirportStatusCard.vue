<script setup lang="ts">
import { computed } from 'vue'

import type { AirportSnapshot } from '@/types/airport'

const props = defineProps<{
  airportOnline: boolean
  error: string | null
  loading: boolean
  snapshot: AirportSnapshot | null
}>()

const airportConnectionLabel = computed(() => (props.airportOnline ? 'Online' : 'Offline'))
const modeLabel = computed(() => {
  if (!props.snapshot) {
    return 'Unbekannt'
  }

  return props.snapshot.state.mode === 'ON' ? 'An' : 'Aus'
})
const lightsLabel = computed(() => {
  if (!props.snapshot || !props.airportOnline) {
    return 'Aus'
  }

  return props.snapshot.state.lightsOn ? 'An' : 'Aus'
})
</script>

<template>
  <section class="status-card" aria-labelledby="airport-status-title">
    <div class="status-card__header">
      <div>
        <p class="status-card__eyebrow">Flughafen</p>
        <h2 id="airport-status-title" class="status-card__title">Flughafenstatus</h2>
      </div>
      <p class="status-card__connection">
        <span
          class="status-card__connection-dot"
          :class="airportOnline ? 'status-card__connection-dot--online' : 'status-card__connection-dot--offline'"
          aria-hidden="true"
        ></span>
        {{ airportConnectionLabel }}
      </p>
    </div>

    <p v-if="loading" class="status-card__meta">Lade Status ...</p>
    <p v-else-if="error" class="status-card__meta status-card__meta--warning">{{ error }}</p>

    <div class="status-card__grid">
      <article class="status-card__item">
        <span class="status-card__label">Modus</span>
        <strong class="status-card__value">{{ modeLabel }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Landelichter</span>
        <strong class="status-card__value">{{ lightsLabel }}</strong>
      </article>
    </div>
  </section>
</template>
