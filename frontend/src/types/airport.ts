export type AirportMode = 'ON' | 'OFF'

export interface AirportStatePayload {
  mode: AirportMode
  lightsOn: boolean
  online: boolean
}

export interface AirportSnapshot {
  state: AirportStatePayload
  brokerConnected: boolean
  updatedAt: string
}
