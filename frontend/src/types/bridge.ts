export type BridgeMode = 'AUTO' | 'MANUAL_OPEN' | 'MANUAL_CLOSE'

export interface BridgeSnapshot {
  mode: BridgeMode
  isPhysicallyOpen: boolean
  brokerConnected: boolean
  espOnline: boolean
  updatedAt: string
}
