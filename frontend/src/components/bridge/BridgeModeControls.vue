<script setup lang="ts">
import type { BridgeMode } from '@/types/bridge'

const props = defineProps<{
  currentMode: BridgeMode | null
  submittingMode: BridgeMode | null
}>()

const emit = defineEmits<{
  setMode: [mode: BridgeMode]
}>()

const modes: Array<{ value: BridgeMode; label: string; description: string }> = [
  { value: 'AUTO', label: 'Auto', description: 'Sensoren steuern die Brücke automatisch.' },
  { value: 'MANUAL_OPEN', label: 'Hochfahren', description: 'Brücke wird sicher hochgefahren (max. 1x).' },
  { value: 'MANUAL_CLOSE', label: 'Runterfahren', description: 'Brücke wird sicher runtergefahren (max. 1x).' },
]
</script>

<template>
  <section class="controls" aria-labelledby="bridge-controls-title">
    <div class="controls__header">
      <div>
        <p class="controls__eyebrow">Steuerung</p>
        <h2 id="bridge-controls-title" class="controls__title">Brückenmodus</h2>
      </div>
      <span class="controls__mode">{{ currentMode ?? 'unbekannt' }}</span>
    </div>

    <div class="controls__buttons">
      <button
        v-for="mode in modes"
        :key="mode.value"
        class="controls__button"
        :class="{
          'controls__button--active': currentMode === mode.value,
          'controls__button--pending': submittingMode === mode.value,
        }"
        type="button"
        :disabled="submittingMode !== null"
        @click="emit('setMode', mode.value)"
      >
        <span class="controls__button-label">{{ mode.label }}</span>
        <span class="controls__button-description">{{ mode.description }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.controls {
  display: grid;
  gap: 20px;
  border: 1px solid #d9e0e2;
  border-radius: 8px;
  padding: 24px;
  background: #ffffff;
}

.controls__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.controls__eyebrow {
  margin: 0 0 4px;
  color: #357266;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.controls__title {
  margin: 0;
  color: #172026;
  font-size: 1.125rem;
  font-weight: 800;
}

.controls__mode {
  border: 1px solid #d8dfe2;
  border-radius: 999px;
  padding: 6px 10px;
  color: #42525b;
  background: #f5f7f8;
  font-size: 0.8125rem;
  font-weight: 700;
}

.controls__buttons {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.controls__button {
  display: grid;
  gap: 8px;
  min-height: 124px;
  border: 1px solid #d9e0e2;
  border-radius: 8px;
  padding: 16px;
  color: #172026;
  background: #f8fafb;
  text-align: left;
  cursor: pointer;
}

.controls__button:disabled {
  cursor: wait;
  opacity: 0.75;
}

.controls__button--active {
  border-color: #357266;
  background: #e8f4ee;
}

.controls__button--pending {
  border-color: #6a7d86;
  background: #edf1f3;
}

.controls__button-label {
  font-size: 1rem;
  font-weight: 800;
}

.controls__button-description {
  color: #5c6870;
  font-size: 0.875rem;
  line-height: 1.4;
}

@media (max-width: 840px) {
  .controls__buttons {
    grid-template-columns: 1fr;
  }
}
</style>
