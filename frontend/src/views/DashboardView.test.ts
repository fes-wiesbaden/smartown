import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import DashboardView from './DashboardView.vue'

const fetchMock = vi.fn()

/**
 * Einfache Test-Doppelklasse fuer Live-Updates ohne echten Browser-Socket.
 */
class MockWebSocket {
  static instances: MockWebSocket[] = []

  url: string
  onmessage: ((event: MessageEvent<string>) => void) | null = null
  onerror: (() => void) | null = null
  onclose: (() => void) | null = null
  readyState = 1

  constructor(url: string) {
    this.url = url
    MockWebSocket.instances.push(this)
  }

  /**
   * Simuliert ein geordnetes Schliessen der Verbindung im Test.
   */
  close() {
    this.onclose?.()
  }
}

describe('DashboardView', () => {
  /**
   * Stubt REST und WebSocket fuer jeden Testlauf deterministisch neu.
   */
  beforeEach(() => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ({
        state: {
          mode: 'AUTO',
          lightState: 'ON',
          lux: 12.5,
          online: true,
          thresholdLux: 50,
        },
        lastEvent: {
          type: 'LIGHT_STATE_CHANGED',
          lightState: 'ON',
          reason: 'LOW_LUX',
        },
        brokerConnected: true,
        updatedAt: '2026-04-23T09:00:00Z',
      }),
    })
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('WebSocket', MockWebSocket)
    MockWebSocket.instances = []
  })

  /**
   * Raeumt globale Stubs nach jedem Testlauf auf.
   */
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  /**
   * Prueft, dass das Dashboard den Snapshot rendert und Moduswechsel per REST ausloest.
   */
  it('renders the live lantern status and sends mode updates', async () => {
    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('Kontrollzentrum')
    expect(wrapper.text()).toContain('Laternen')
    expect(wrapper.text()).toContain('ESP32 online')
    expect(wrapper.text()).toContain('12.5 lx')

    await wrapper.get('button').trigger('click')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/lanterns',
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/lanterns/mode',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ mode: 'AUTO' }),
      }),
    )
  })

  /**
   * Prueft, dass ein offline gemeldeter ESP32 nicht weiter als online angezeigt wird.
   */
  it('renders the esp32 as offline when the snapshot says offline', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        state: {
          mode: 'AUTO',
          lightState: 'OFF',
          lux: null,
          online: false,
          thresholdLux: 50,
        },
        lastEvent: {
          type: 'SYSTEM_START',
          lightState: 'OFF',
          reason: 'SYSTEM_START',
        },
        brokerConnected: true,
        updatedAt: '2026-04-23T09:00:00Z',
      }),
    })

    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('ESP32 offline')
    expect(wrapper.text()).not.toContain('ESP32 online')
  })
})
