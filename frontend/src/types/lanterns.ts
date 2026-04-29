/** Unterstuetzte Modi fuer die Laternensteuerung. */
export type LanternMode = 'AUTO' | 'FORCED_ON' | 'FORCED_OFF'
/** Technischer Zustand des Lampenausgangs. */
export type LightState = 'ON' | 'OFF'
/** Fachliche Ursachen fuer eine Statusaenderung. */
export type LanternReason = 'LOW_LUX' | 'HIGH_LUX' | 'MANUAL_OVERRIDE' | 'SYSTEM_START'

/** Beschreibt den laufenden Zustand der Laternen, wie er vom ESP32 gesendet wird. */
export interface LanternStatePayload {
  mode: LanternMode
  lightState: LightState
  lux: number | null
  online: boolean
  thresholdLux: number | null
}

/** Beschreibt das letzte fachliche Ereignis aus der Firmware. */
export interface LanternEventPayload {
  type: string
  lightState: LightState
  reason: LanternReason
}

/** Kombiniert den aktuellen Zustand mit Broker- und Zeitinformationen. */
export interface LanternSnapshot {
  state: LanternStatePayload
  lastEvent: LanternEventPayload
  brokerConnected: boolean
  updatedAt: string
}
