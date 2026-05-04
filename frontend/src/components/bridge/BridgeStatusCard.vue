<script setup lang="ts">
import { computed } from 'vue'

import type { BridgeSnapshot } from '@/types/bridge'

const props = defineProps<{
  snapshot: BridgeSnapshot | null
  loading: boolean
  error: string | null
  bridgeOnline: boolean
}>()

const modeLabel = computed(() => {
  const mode = props.snapshot?.mode
  if (!mode) {
    return '-'
  }

  if (mode === 'AUTO') {
    return 'Auto'
  }

  return mode === 'MANUAL_OPEN' ? 'Hoch' : 'Runter'
})

const bridgePositionLabel = computed(() => {
  if (!props.snapshot || !props.bridgeOnline) {
    return '/'
  }

  return props.snapshot.isPhysicallyOpen ? 'Oben' : 'Unten'
})

const bridgeConnectionLabel = computed(() => (props.bridgeOnline ? 'Online' : 'Offline'))
</script>

<template>
  <article class="status-card" aria-labelledby="bridge-status-title">
    <div class="status-card__header">
      <div>
        <p class="status-card__eyebrow">Brücke</p>
        <h2 id="bridge-status-title" class="status-card__title">Brückenstatus</h2>
      </div>
      <p class="status-card__connection">
        <span
          class="status-card__connection-dot"
          :class="bridgeOnline ? 'status-card__connection-dot--online' : 'status-card__connection-dot--offline'"
          aria-hidden="true"
        ></span>
        {{ bridgeConnectionLabel }}
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
        <span class="status-card__label">Zustand</span>
        <strong class="status-card__value">{{ bridgePositionLabel }}</strong>
      </article>
    </div>
  </article>
</template>

<style scoped>
.status-card {
  display: grid;
  height: 100%;
  align-content: start;
  gap: 20px;
  border: 1px solid var(--theme-card-border);
  border-radius: 14px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 40px rgba(96, 53, 250, 0.08);
}

.status-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.status-card__eyebrow {
  margin: 0 0 4px;
  color: var(--theme-accent);
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

.status-card__notice {
  margin: 0;
  color: var(--theme-muted);
  font-weight: 600;
}

.status-card__notice--error {
  color: #b42318;
}

.status-card__connection {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #172026;
  font-size: 0.875rem;
  font-weight: 800;
}

.status-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
}

.status-card__item {
  display: grid;
  gap: 6px;
  min-height: 96px;
  border: 1px solid var(--theme-surface-border);
  border-radius: 10px;
  padding: 16px;
  background: var(--theme-surface);
}

.status-card__label {
  color: var(--theme-muted);
  font-size: 0.8125rem;
  font-weight: 700;
  text-transform: uppercase;
}

.status-card__value {
  color: #172026;
  font-size: 1rem;
  font-weight: 800;
}

.status-card__connection-dot {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 50%;
}

.status-card__connection-dot--online {
  background: #16a34a;
}

.status-card__connection-dot--offline {
  background: #dc2626;
}

@media (max-width: 840px) {
  .status-card__header {
    flex-direction: column;
  }

  .status-card__grid {
    grid-template-columns: 1fr;
  }
}
</style>
