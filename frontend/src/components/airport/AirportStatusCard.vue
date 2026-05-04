<script setup lang="ts">
import { computed } from 'vue'

import type { AirportMode } from '@/types/airport'

const props = defineProps<{
  mode: AirportMode
}>()

const airportConnectionLabel = 'Nicht verbunden'
const lightsLabel = computed(() => (props.mode === 'ON' ? 'Aktiv' : 'Aus'))
</script>

<template>
  <section class="status-card" aria-labelledby="airport-status-title">
    <div class="status-card__header">
      <div>
        <p class="status-card__eyebrow">Flughafen</p>
        <h2 id="airport-status-title" class="status-card__title">Flughafenstatus</h2>
      </div>
      <p class="status-card__connection">
        <span class="status-card__connection-dot status-card__connection-dot--offline" aria-hidden="true"></span>
        {{ airportConnectionLabel }}
      </p>
    </div>

    <div class="status-card__grid">
      <article class="status-card__item">
        <span class="status-card__label">Modus</span>
        <strong class="status-card__value">{{ mode === 'ON' ? 'An' : 'Aus' }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Landelichter</span>
        <strong class="status-card__value">{{ lightsLabel }}</strong>
      </article>
    </div>
  </section>
</template>
