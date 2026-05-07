<script setup lang="ts">
import { computed } from 'vue'

import type { LanternLuxHistoryPoint } from '@/types/lanterns'

const props = defineProps<{
  error: string | null
  loading: boolean
  points: LanternLuxHistoryPoint[]
}>()

const chartWidth = 760
const chartHeight = 280
const chartPadding = {
  bottom: 36,
  left: 76,
  right: 20,
  top: 18,
}
const chartPlotWidth = chartWidth - chartPadding.left - chartPadding.right
const chartPlotHeight = chartHeight - chartPadding.top - chartPadding.bottom

type ChartPoint = LanternLuxHistoryPoint & {
  measuredAtMs: number
  x: number
  y: number
}

const validPoints = computed(() =>
  [...(Array.isArray(props.points) ? props.points : [])]
    .map((point) => ({
      ...point,
      measuredAtMs: Date.parse(point.measuredAt),
    }))
    .filter((point) => Number.isFinite(point.lux) && !Number.isNaN(point.measuredAtMs))
    .sort((left, right) => left.measuredAtMs - right.measuredAtMs),
)

const luxDomain = computed(() => {
  if (validPoints.value.length === 0) {
    return {
      max: 100,
      min: 0,
      span: 100,
    }
  }

  const luxValues = validPoints.value.map((point) => point.lux)
  const minLux = Math.min(...luxValues)
  const maxLux = Math.max(...luxValues)
  const padding = minLux === maxLux ? Math.max(5, Math.abs(maxLux) * 0.1 || 5) : Math.max((maxLux - minLux) * 0.12, 1)
  const min = Math.max(0, minLux - padding)
  const max = maxLux + padding

  return {
    max,
    min,
    span: max - min || 1,
  }
})

const timeDomain = computed(() => {
  if (validPoints.value.length === 0) {
    return {
      end: 1,
      span: 1,
      start: 0,
    }
  }

  const start = validPoints.value[0].measuredAtMs
  const end = validPoints.value[validPoints.value.length - 1].measuredAtMs
  const span = end - start || 1

  return {
    end,
    span,
    start,
  }
})

const chartPoints = computed<ChartPoint[]>(() =>
  validPoints.value.map((point) => ({
    ...point,
    x:
      validPoints.value.length === 1
        ? chartPadding.left + chartPlotWidth / 2
        : chartPadding.left + ((point.measuredAtMs - timeDomain.value.start) / timeDomain.value.span) * chartPlotWidth,
    y: chartPadding.top + ((luxDomain.value.max - point.lux) / luxDomain.value.span) * chartPlotHeight,
  })),
)

const linePath = computed(() => {
  if (chartPoints.value.length === 0) {
    return ''
  }

  return chartPoints.value
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`)
    .join(' ')
})

const areaPath = computed(() => {
  if (chartPoints.value.length === 0) {
    return ''
  }

  const firstPoint = chartPoints.value[0]
  const lastPoint = chartPoints.value[chartPoints.value.length - 1]
  return `${linePath.value} L ${lastPoint.x.toFixed(2)} ${(chartHeight - chartPadding.bottom).toFixed(2)} L ${firstPoint.x.toFixed(2)} ${(chartHeight - chartPadding.bottom).toFixed(2)} Z`
})

const yAxisTicks = computed(() => {
  const steps = 4
  return Array.from({ length: steps + 1 }, (_, index) => {
    const ratio = index / steps
    const lux = luxDomain.value.max - ratio * luxDomain.value.span
    return {
      key: `y-${index}`,
      label: `${lux.toFixed(1)} lx`,
      y: chartPadding.top + ratio * chartPlotHeight,
    }
  })
})

const xAxisTicks = computed(() => {
  if (chartPoints.value.length === 0) {
    return []
  }

  const tickCount = Math.min(6, chartPoints.value.length)
  return Array.from({ length: tickCount }, (_, index) => {
    const ratio = tickCount === 1 ? 0.5 : index / (tickCount - 1)
    const measuredAtMs = timeDomain.value.start + ratio * timeDomain.value.span
    return {
      key: `x-${index}`,
      label: formatTimeLabel(measuredAtMs, timeDomain.value.span),
      x: chartPadding.left + ratio * chartPlotWidth,
    }
  })
})

const latestLuxLabel = computed(() => {
  const latestPoint = validPoints.value[validPoints.value.length - 1]
  return latestPoint ? `${latestPoint.lux.toFixed(1)} lx` : '-'
})

const measurementCountLabel = computed(() => `${validPoints.value.length}`)

const rangeLabel = computed(() => {
  if (validPoints.value.length === 0) {
    return 'Noch keine Messwerte'
  }

  const firstPoint = validPoints.value[0]
  const lastPoint = validPoints.value[validPoints.value.length - 1]
  return `${formatRangeLabel(firstPoint.measuredAtMs)} bis ${formatRangeLabel(lastPoint.measuredAtMs)}`
})

function formatRangeLabel(measuredAtMs: number) {
  return new Intl.DateTimeFormat('de-DE', {
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    month: '2-digit',
  }).format(new Date(measuredAtMs))
}

function formatTimeLabel(measuredAtMs: number, spanMs: number) {
  const formatter =
    spanMs < 86_400_000
      ? new Intl.DateTimeFormat('de-DE', {
          hour: '2-digit',
          minute: '2-digit',
        })
      : new Intl.DateTimeFormat('de-DE', {
          day: '2-digit',
          hour: '2-digit',
          month: '2-digit',
        })

  return formatter.format(new Date(measuredAtMs))
}
</script>

<template>
  <section class="status-card lux-chart-card" aria-labelledby="lantern-lux-chart-title">
    <div class="status-card__header">
      <div>
        <p class="status-card__eyebrow">Verlauf</p>
        <h2 id="lantern-lux-chart-title" class="status-card__title">Lux-Historie</h2>
      </div>
      <p class="lux-chart-card__range">{{ rangeLabel }}</p>
    </div>

    <p v-if="loading" class="status-card__notice">Lux-Historie wird geladen.</p>
    <p v-else-if="error" class="status-card__notice status-card__notice--error">{{ error }}</p>

    <div class="lux-chart-card__stats" aria-label="Lux-Zusammenfassung">
      <article class="status-card__item">
        <span class="status-card__label">Messwerte</span>
        <strong class="status-card__value">{{ measurementCountLabel }}</strong>
      </article>
      <article class="status-card__item">
        <span class="status-card__label">Letzter Wert</span>
        <strong class="status-card__value">{{ latestLuxLabel }}</strong>
      </article>
    </div>

    <p v-if="!loading && points.length === 0" class="status-card__notice">
      Noch keine Luxwerte in der Datenbank vorhanden.
    </p>

    <div v-else class="lux-chart-card__canvas">
      <svg
        class="lux-chart-card__svg"
        :viewBox="`0 0 ${chartWidth} ${chartHeight}`"
        preserveAspectRatio="none"
        role="img"
        aria-label="Luxwerte ueber Zeit"
      >
        <g aria-hidden="true">
          <line
            v-for="tick in yAxisTicks"
            :key="tick.key"
            class="lux-chart-card__grid-line"
            :x1="chartPadding.left"
            :x2="chartWidth - chartPadding.right"
            :y1="tick.y"
            :y2="tick.y"
          />
          <text
            v-for="tick in yAxisTicks"
            :key="`${tick.key}-label`"
            class="lux-chart-card__axis-label"
            :x="chartPadding.left - 10"
            :y="tick.y + 4"
            text-anchor="end"
          >
            {{ tick.label }}
          </text>
          <text
            v-for="tick in xAxisTicks"
            :key="tick.key"
            class="lux-chart-card__axis-label"
            :x="tick.x"
            :y="chartHeight - 10"
            text-anchor="middle"
          >
            {{ tick.label }}
          </text>
        </g>

        <path v-if="areaPath" class="lux-chart-card__area" :d="areaPath" />
        <path v-if="linePath" class="lux-chart-card__line" :d="linePath" />
      </svg>
    </div>
  </section>
</template>

<style scoped>
.lux-chart-card {
  gap: 18px;
}

.lux-chart-card__range {
  margin: 0;
  color: var(--theme-muted);
  font-size: 0.8125rem;
  font-weight: 700;
  text-align: right;
}

.lux-chart-card__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.lux-chart-card__canvas {
  min-height: 320px;
  border: 1px solid var(--theme-surface-border);
  border-radius: 14px;
  padding: 16px 12px 8px;
  background:
    linear-gradient(180deg, rgba(96, 53, 250, 0.12), rgba(96, 53, 250, 0.02)),
    #fcfbff;
}

.lux-chart-card__svg {
  display: block;
  width: 100%;
  height: 280px;
}

.lux-chart-card__grid-line {
  stroke: rgba(106, 125, 134, 0.2);
  stroke-width: 1;
}

.lux-chart-card__axis-label {
  fill: #51636c;
  font-size: 0.75rem;
  font-weight: 700;
}

.lux-chart-card__area {
  fill: rgba(96, 53, 250, 0.14);
}

.lux-chart-card__line {
  fill: none;
  stroke: #6035fa;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

@media (max-width: 840px) {
  .lux-chart-card__range {
    text-align: left;
  }

  .lux-chart-card__stats {
    grid-template-columns: 1fr;
  }

  .lux-chart-card__canvas {
    min-height: 280px;
    padding: 14px 8px 8px;
  }

  .lux-chart-card__svg {
    height: 240px;
  }
}
</style>
