<script setup lang="ts">
import type { BridgeSnapshot } from '@/types/bridge'

defineProps<{
  snapshot: BridgeSnapshot | null
  loading: boolean
  error: string | null
  bridgeOnline: boolean
}>()
</script>

<template>
  <article class="status-card" aria-labelledby="bridge-status-title">
    <div class="status-card__header">
      <div>
        <p class="status-card__eyebrow">Brücke</p>
        <h2 id="bridge-status-title" class="status-card__title">Brückenstatus</h2>
      </div>
      <div v-if="loading" class="status-card__badge status-card__badge--loading">Lädt…</div>
      <div v-else-if="error" class="status-card__badge status-card__badge--error">Fehler</div>
      <div v-else class="status-card__badge status-card__badge--success">Aktiv</div>
    </div>

    <dl class="status-list">
      <div class="status-list__item">
        <dt class="status-list__term">Brücke (ESP32)</dt>
        <dd class="status-list__value">
          <span class="status-indicator" :class="bridgeOnline ? 'status-indicator--on' : 'status-indicator--off'"></span>
          {{ bridgeOnline ? 'Online' : 'Offline' }}
        </dd>
      </div>
      
      <div class="status-list__item">
        <dt class="status-list__term">Aktueller Zustand</dt>
        <dd class="status-list__value status-list__value--highlight">
          {{ snapshot?.isPhysicallyOpen ? 'OBEN' : 'UNTEN' }}
        </dd>
      </div>
    </dl>
  </article>
</template>

<style scoped>
.status-card {
  display: flex;
  flex-direction: column;
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

.status-card__badge {
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.status-card__badge--loading {
  color: var(--theme-muted);
  background: #f5f7f8;
}

.status-card__badge--error {
  color: var(--theme-offline);
  background: var(--theme-offline-bg);
}

.status-card__badge--success {
  color: var(--theme-accent-strong);
  background: var(--theme-accent-soft);
}

.status-list {
  display: grid;
  gap: 16px;
  margin: 0;
}

.status-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f5f7f8;
  padding-bottom: 16px;
}

.status-list__item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.status-list__term {
  color: var(--theme-muted);
  font-size: 0.875rem;
  font-weight: 600;
}

.status-list__value {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #172026;
  font-size: 0.875rem;
  font-weight: 700;
}

.status-list__value--highlight {
  font-weight: 800;
  color: var(--theme-accent);
}

.status-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-indicator--on {
  background: #16a34a;
}

.status-indicator--off {
  background: #dc2626;
}
</style>
