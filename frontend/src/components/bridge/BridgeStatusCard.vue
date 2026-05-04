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
