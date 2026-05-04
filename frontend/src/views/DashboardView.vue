<script setup lang="ts">
import { computed } from 'vue'

import BridgeModeControls from '@/components/bridge/BridgeModeControls.vue'
import BridgeStatusCard from '@/components/bridge/BridgeStatusCard.vue'
import LanternModeControls from '@/components/lanterns/LanternModeControls.vue'
import LanternStatusCard from '@/components/lanterns/LanternStatusCard.vue'
import { useBridge } from '@/composables/useBridge'
import { useLanterns } from '@/composables/useLanterns'

const dashboardLogoUrl = '/smartown-logo.png'

/**
 * Bindet Snapshot, Live-Status und Moduswechsel in die Dashboard-Ansicht ein.
 */
const { brokerConnected, error, lanternOnline, liveConnected: lanternLiveConnected, loading, setMode, snapshot, submittingMode } = useLanterns()
const { bridgeMode, submittingBridgeMode, setBridgeMode, snapshot: bridgeSnapshot, loading: bridgeLoading, error: bridgeError, brokerConnected: bridgeBroker, bridgeOnline, liveConnected: bridgeLiveConnected } = useBridge()

const lanternControlsEnabled = computed(() => brokerConnected.value && lanternOnline.value)
const bridgeControlsEnabled = computed(() => bridgeBroker.value && bridgeOnline.value)
const liveUpdatesActive = computed(() => lanternLiveConnected.value || bridgeLiveConnected.value)
const mqttStates = computed(() =>
  [snapshot.value?.brokerConnected, bridgeSnapshot.value?.brokerConnected].filter(
    (state): state is boolean => state !== undefined,
  ),
)
const mqttConnected = computed(() => mqttStates.value.length > 0 && mqttStates.value.every(Boolean))
const mqttStatusLabel = computed(() => {
  if (mqttStates.value.length === 0) {
    return 'Warte auf Broker'
  }

  return mqttConnected.value ? 'Verbunden' : 'Getrennt'
})

/**
 * Zeigt den Stand der Stadtmodule, wobei Laternen und Bruecke bereits live angebunden sind.
 */
const modules = computed(() => [
  {
    name: 'MQTT Broker',
    status: mqttStatusLabel.value,
    eyebrow: 'System',
    detail: 'Zentrale Verbindung fuer alle Module',
    featured: true,
    online: mqttConnected.value,
  },
  { name: 'Bruecke', status: 'Offen', eyebrow: 'Stadtmodul', featured: false },
  { name: 'Flughafen', status: 'Offen', eyebrow: 'Stadtmodul', featured: false },
  {
    name: 'Laternen',
    status: !snapshot.value ? 'Warte auf ESP32' : lanternOnline.value ? 'ESP32 online' : 'ESP32 offline',
    eyebrow: 'Stadtmodul',
    featured: false,
  },
])
</script>

<template>
  <main class="dashboard">
    <header class="dashboard__header">
      <div class="dashboard__intro">
        <img
          class="dashboard__logo"
          :src="dashboardLogoUrl"
          alt="SmarTown Logo"
          width="72"
          height="72"
          fetchpriority="high"
        />
        <h1 class="dashboard__title">Kontrollzentrum</h1>
        <span class="dashboard__live" :class="{ 'dashboard__live--offline': !liveUpdatesActive }">
          <span class="dashboard__live-dot" aria-hidden="true"></span>
          {{ liveUpdatesActive ? 'Live' : 'Live aus' }}
        </span>
      </div>
    </header>

    <section class="dashboard__section" aria-label="Stadtmodule">
      <div class="module-grid">
        <article
          v-for="module in modules"
          :key="module.name"
          class="module-card"
          :class="{
            'module-card--broker': module.featured,
            'module-card--broker-offline': module.featured && !module.online,
          }"
        >
          <p class="module-card__eyebrow">{{ module.eyebrow }}</p>
          <div v-if="module.featured" class="module-card__broker-head">
            <h2 class="module-card__title">{{ module.name }}</h2>
            <p
              class="module-card__status module-card__status--broker"
              :class="{ 'module-card__status--broker-offline': !module.online }"
            >
              {{ module.status }}
            </p>
          </div>
          <template v-else>
            <h2 class="module-card__title">{{ module.name }}</h2>
            <p class="module-card__status">{{ module.status }}</p>
          </template>
          <p v-if="module.detail" class="module-card__detail">{{ module.detail }}</p>
        </article>
      </div>
    </section>

    <section class="dashboard__section dashboard__section--feature" aria-label="Laternen">
      <LanternStatusCard
        :error="error"
        :loading="loading"
        :snapshot="snapshot"
      />
      <LanternModeControls
        :controls-enabled="lanternControlsEnabled"
        :current-mode="snapshot?.state.mode ?? null"
        :submitting-mode="submittingMode"
        @set-mode="setMode"
      />
    </section>

    <section class="dashboard__section dashboard__section--feature" aria-label="Brücke">
      <BridgeStatusCard
        :bridge-online="bridgeOnline"
        :error="bridgeError"
        :loading="bridgeLoading"
        :snapshot="bridgeSnapshot"
      />
      <BridgeModeControls
        :controls-enabled="bridgeControlsEnabled"
        :current-mode="bridgeMode"
        :submitting-mode="submittingBridgeMode"
        @set-mode="setBridgeMode"
      />
    </section>
  </main>
</template>

<style scoped>
.dashboard {
  min-height: 100vh;
  padding: 32px;
  color: #172026;
  background:
    radial-gradient(circle at top left, rgba(96, 53, 250, 0.14), transparent 26%),
    radial-gradient(circle at top right, rgba(96, 53, 250, 0.1), transparent 24%),
    #f6f3ff;
}

.dashboard__header {
  margin: 0 auto 32px;
  max-width: 1120px;
}

.dashboard__intro {
  display: grid;
  justify-items: start;
}

.dashboard__logo {
  display: block;
  width: 72px;
  height: 72px;
  margin-bottom: 10px;
  object-fit: contain;
  filter: drop-shadow(0 12px 24px rgba(96, 53, 250, 0.16));
}

.dashboard__title {
  margin: 0;
  color: #172026;
  font-size: 2rem;
  font-weight: 800;
  text-wrap: balance;
}

.dashboard__live {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  border: 1px solid #dc2626;
  border-radius: 999px;
  padding: 6px 14px;
  color: #ffffff;
  background: #dc2626;
  font-size: 0.9375rem;
  font-weight: 800;
  text-transform: uppercase;
  box-shadow: 0 10px 24px rgba(220, 38, 38, 0.22);
  transform-origin: left center;
  animation: dashboard-live-badge 1.9s ease-in-out infinite;
}

.dashboard__live-dot {
  position: relative;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: currentColor;
  animation: dashboard-live-pulse 1.6s ease-out infinite;
}

.dashboard__live-dot::after {
  content: '';
  position: absolute;
  inset: -6px;
  border: 2px solid currentColor;
  border-radius: 50%;
  opacity: 0.45;
  animation: dashboard-live-ripple 1.6s ease-out infinite;
}

.dashboard__live--offline {
  border-color: #d8dfe2;
  color: #5c6870;
  background: #f5f7f8;
  box-shadow: none;
  animation-play-state: paused;
}

.dashboard__live--offline .dashboard__live-dot,
.dashboard__live--offline .dashboard__live-dot::after {
  animation-play-state: paused;
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
  position: relative;
  overflow: hidden;
  border: 1px solid var(--theme-card-border);
  border-radius: 14px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  box-shadow: 0 16px 40px rgba(96, 53, 250, 0.08);
}

.module-card--broker {
  border-color: rgba(96, 53, 250, 0.36);
  background:
    radial-gradient(circle at top right, rgba(96, 53, 250, 0.24), transparent 40%),
    radial-gradient(circle at bottom left, rgba(150, 120, 255, 0.2), transparent 44%),
    linear-gradient(160deg, rgba(244, 238, 255, 0.98) 0%, rgba(232, 220, 255, 0.95) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.76),
    0 22px 46px rgba(96, 53, 250, 0.16);
}

.module-card--broker-offline {
  background:
    radial-gradient(circle at top right, rgba(96, 53, 250, 0.2), transparent 40%),
    radial-gradient(circle at bottom left, rgba(150, 120, 255, 0.16), transparent 44%),
    linear-gradient(160deg, rgba(243, 237, 255, 0.98) 0%, rgba(235, 226, 255, 0.95) 100%);
}

.module-card--broker::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.module-card__eyebrow {
  margin: 0 0 10px;
  color: var(--theme-accent);
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.module-card--broker .module-card__eyebrow {
  color: var(--theme-accent);
}

.module-card__title {
  margin: 0 0 8px;
  color: #172026;
  font-size: 1rem;
  font-weight: 800;
  text-wrap: balance;
}

.module-card__broker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.module-card__broker-head .module-card__title {
  margin: 0;
}

.module-card--broker .module-card__title {
  color: #172026;
}

.module-card__status {
  margin: 0;
  color: var(--theme-muted);
  font-weight: 600;
}

.module-card__status--broker {
  display: inline-flex;
  flex-shrink: 0;
  width: fit-content;
  border: 1px solid var(--theme-accent-border);
  border-radius: 999px;
  padding: 6px 12px;
  color: var(--theme-accent-strong);
  background: rgba(255, 255, 255, 0.44);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55);
  font-weight: 800;
}

.module-card__status--broker-offline {
  border-color: rgba(177, 160, 235, 0.8);
  color: #6d5aa8;
  background: rgba(247, 244, 255, 0.92);
}

.module-card__detail {
  position: relative;
  z-index: 1;
  margin: 0;
  max-width: 20ch;
  color: #5c6870;
  font-size: 0.875rem;
  line-height: 1.45;
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

  .module-grid {
    grid-template-columns: 1fr;
  }

  .module-card__broker-head {
    align-items: flex-start;
    flex-direction: column;
  }
}

@keyframes dashboard-live-pulse {
  0%,
  100% {
    transform: scale(0.92);
    opacity: 0.9;
  }

  50% {
    transform: scale(1.08);
    opacity: 1;
  }
}

@keyframes dashboard-live-badge {
  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.045);
  }
}

@keyframes dashboard-live-ripple {
  0% {
    transform: scale(0.72);
    opacity: 0.5;
  }

  70%,
  100% {
    transform: scale(1.2);
    opacity: 0;
  }
}
</style>
