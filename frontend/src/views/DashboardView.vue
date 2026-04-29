<script setup lang="ts">
import { computed } from 'vue'

import LanternModeControls from '@/components/lanterns/LanternModeControls.vue'
import LanternStatusCard from '@/components/lanterns/LanternStatusCard.vue'
import { useLanterns } from '@/composables/useLanterns'

/**
 * Bindet Snapshot, Live-Status und Moduswechsel in die Dashboard-Ansicht ein.
 */
const { brokerConnected, error, lanternOnline, loading, setMode, snapshot, submittingMode } = useLanterns()

/**
 * Zeigt den Stand der Stadtmodule, wobei nur die Laternen bereits am MQTT-MVP haengen.
 */
const modules = computed(() => [
  { name: 'Skilift', status: 'MVP offen' },
  { name: 'Bruecke', status: 'MVP offen' },
  { name: 'Flughafen', status: 'MVP offen' },
  {
    name: 'Laternen',
    status: !snapshot.value ? 'Warte auf ESP32' : lanternOnline.value ? 'ESP32 online' : 'ESP32 offline',
  },
])
</script>

<template>
  <main class="dashboard">
    <header class="dashboard__header">
      <div>
        <p class="dashboard__eyebrow">SmarTown</p>
        <h1 class="dashboard__title">Kontrollzentrum</h1>
      </div>
      <span class="dashboard__status" :class="{ 'dashboard__status--offline': !brokerConnected }">
        {{ brokerConnected ? 'MQTT verbunden' : 'MQTT getrennt' }}
      </span>
    </header>

    <section class="dashboard__section" aria-label="Stadtmodule">
      <div class="module-grid">
        <article v-for="module in modules" :key="module.name" class="module-card">
          <h2 class="module-card__title">{{ module.name }}</h2>
          <p class="module-card__status">{{ module.status }}</p>
        </article>
      </div>
    </section>

    <section class="dashboard__section dashboard__section--feature" aria-label="Laternen MQTT MVP">
      <LanternStatusCard
        :broker-connected="brokerConnected"
        :error="error"
        :loading="loading"
        :snapshot="snapshot"
      />
      <LanternModeControls
        :current-mode="snapshot?.state.mode ?? null"
        :submitting-mode="submittingMode"
        @set-mode="setMode"
      />
    </section>
  </main>
</template>

<style scoped>
.dashboard {
  min-height: 100vh;
  padding: 32px;
  color: #172026;
  background: #f5f7f8;
}

.dashboard__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin: 0 auto 32px;
  max-width: 1120px;
}

.dashboard__eyebrow {
  margin: 0 0 4px;
  color: #357266;
  font-size: 0.875rem;
  font-weight: 700;
  text-transform: uppercase;
}

.dashboard__title {
  margin: 0;
  color: #172026;
  font-size: 2rem;
  font-weight: 800;
}

.dashboard__status {
  border: 1px solid #bad4ca;
  border-radius: 999px;
  padding: 6px 12px;
  color: #1f5f4b;
  background: #e8f4ee;
  font-size: 0.875rem;
  font-weight: 700;
}

.dashboard__status--offline {
  border-color: #f1d2b6;
  color: #9a3412;
  background: #fff2e8;
}

.dashboard__section {
  margin: 0 auto 24px;
  max-width: 1120px;
}

.dashboard__section--feature {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(300px, 1fr);
  gap: 16px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.module-card {
  border: 1px solid #d9e0e2;
  border-radius: 8px;
  padding: 20px;
  background: #ffffff;
}

.module-card__title {
  margin: 0 0 8px;
  color: #172026;
  font-size: 1rem;
  font-weight: 800;
}

.module-card__status {
  margin: 0;
  color: #5c6870;
  font-weight: 600;
}

@media (max-width: 920px) {
  .dashboard__section--feature {
    grid-template-columns: 1fr;
  }

  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .dashboard {
    padding: 20px;
  }

  .dashboard__header {
    align-items: stretch;
    flex-direction: column;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
