<script setup lang="ts">
import { computed } from 'vue'

import type { LanternSnapshot } from '@/types/lanterns'

/**
 * Enthalten den letzten Snapshot plus Lade- und Fehlerzustand fuer die Statuskarte.
 */
const props = defineProps<{
  brokerConnected: boolean
  error: string | null
  loading: boolean
  snapshot: LanternSnapshot | null
}>()

/**
 * Formatiert den Online-Status des ESP32 fuer die Oberflaeche.
 */
const onlineLabel = computed(() => {
  if (!props.snapshot) {
    return 'Warte auf Daten'
  }

  return props.snapshot.state.online ? 'ESP32 online' : 'ESP32 offline'
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
 * Formatiert den zuletzt gemeldeten Luxwert mit Einheit.
 */
const luxLabel = computed(() => {
  const lux = props.snapshot?.state.lux
  return lux === null || lux === undefined ? '-' : `${lux.toFixed(1)} lx`
})

/**
 * Formatiert den aktuell gemeldeten Schwellwert mit Einheit.
 */
const thresholdLabel = computed(() => {
  const threshold = props.snapshot?.state.thresholdLux
  return threshold === null || threshold === undefined ? '-' : `${threshold.toFixed(1)} lx`
})
</script>

<template>
  <section class="status-card" aria-labelledby="lantern-status-title">
    <div class="status-card__header">
      <div>
        <p class="status-card__eyebrow">Laternen</p>
        <h2 id="lantern-status-title" class="status-card__title">MQTT Status</h2>
      </div>
      <div class="status-card__badges">
        <span class="status-card__badge" :class="{ 'status-card__badge--online': brokerConnected }">
          {{ brokerConnected ? 'Broker verbunden' : 'Broker getrennt' }}
        </span>
        <span class="status-card__badge" :class="{ 'status-card__badge--online': snapshot?.state.online }">
          {{ onlineLabel }}
        </span>
      </div>
    </div>

    <p v-if="loading" class="status-card__notice">Snapshot wird geladen.</p>
    <p v-else-if="error" class="status-card__notice status-card__notice--error">{{ error }}</p>

    <div v-if="snapshot" class="status-card__grid">
      <article class="status-card__item">
        <span class="status-card__label">Modus</span>
        <strong class="status-card__value">{{ snapshot.state.mode }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Licht</span>
        <strong class="status-card__value">{{ lightLabel }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Lux</span>
        <strong class="status-card__value">{{ luxLabel }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Schwellwert</span>
        <strong class="status-card__value">{{ thresholdLabel }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Letztes Event</span>
        <strong class="status-card__value">{{ snapshot.lastEvent.type }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Grund</span>
        <strong class="status-card__value">{{ snapshot.lastEvent.reason }}</strong>
      </article>
    </div>
  </section>
</template>

<style scoped>
.status-card {
  display: grid;
  gap: 20px;
  border: 1px solid #d9e0e2;
  border-radius: 8px;
  padding: 24px;
  background: #ffffff;
}

.status-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.status-card__eyebrow {
  margin: 0 0 4px;
  color: #357266;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.status-card__title {
  margin: 0;
  color: #172026;
  font-size: 1.125rem;
  font-weight: 800;
}

.status-card__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.status-card__badge {
  border: 1px solid #d8dfe2;
  border-radius: 999px;
  padding: 6px 10px;
  color: #5c6870;
  background: #f5f7f8;
  font-size: 0.8125rem;
  font-weight: 700;
}

.status-card__badge--online {
  border-color: #bad4ca;
  color: #1f5f4b;
  background: #e8f4ee;
}

.status-card__notice {
  margin: 0;
  color: #5c6870;
  font-weight: 600;
}

.status-card__notice--error {
  color: #b42318;
}

.status-card__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.status-card__item {
  display: grid;
  gap: 6px;
  border: 1px solid #eef2f4;
  border-radius: 8px;
  padding: 16px;
  background: #f8fafb;
}

.status-card__label {
  color: #5c6870;
  font-size: 0.8125rem;
  font-weight: 700;
  text-transform: uppercase;
}

.status-card__value {
  color: #172026;
  font-size: 1rem;
  font-weight: 800;
}

@media (max-width: 840px) {
  .status-card__header {
    flex-direction: column;
  }

  .status-card__badges {
    justify-content: flex-start;
  }

  .status-card__grid {
    grid-template-columns: 1fr;
  }
}
</style>
