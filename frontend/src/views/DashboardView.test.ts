import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const fetchMock = vi.fn()
const lanternSnapshot = {
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
}
const bridgeSnapshot = {
  mode: 'AUTO',
  isPhysicallyOpen: false,
  brokerConnected: true,
  espOnline: true,
  updatedAt: '2026-04-23T09:00:00Z',
}

async function loadDashboardView() {
  return (await import('./DashboardView.vue')).default
}

describe('DashboardView', () => {
  /**
   * Stubt REST und WebSocket fuer jeden Testlauf deterministisch neu.
   */
  beforeEach(() => {
    fetchMock.mockImplementation(async (input: string) => ({
      ok: true,
      json: async () => {
        if (input === '/api/bridge') {
          return bridgeSnapshot
        }
        if (input === '/api/bridge/mode') {
          return null
        }
        return lanternSnapshot
      },
    }))
    vi.stubGlobal('fetch', fetchMock)
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
  it('renders both live modules and sends mode updates', async () => {
    const DashboardView = await loadDashboardView()
    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('Kontrollzentrum')
    expect(wrapper.text()).toContain('Laternen')
    expect(wrapper.text()).toContain('Brücke')
    expect(wrapper.text()).toContain('ESP32 online')
    expect(wrapper.text()).toContain('12.5 lx')
    expect(wrapper.text()).toContain('GESCHLOSSEN')

    await wrapper.get('button').trigger('click')
    await flushPromises()

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
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/bridge',
    )
  })

  /**
   * Prueft, dass ein offline gemeldeter ESP32 nicht weiter als online angezeigt wird.
   */
  it('renders the esp32 as offline when the snapshot says offline', async () => {
    const DashboardView = await loadDashboardView()
    fetchMock.mockImplementationOnce(async () => ({
      ok: true,
      json: async () => ({
        ...lanternSnapshot,
        state: {
          ...lanternSnapshot.state,
          lightState: 'OFF',
          lux: null,
          online: false,
        },
        lastEvent: {
          type: 'SYSTEM_START',
          lightState: 'OFF',
          reason: 'SYSTEM_START',
        },
      }),
    }))

    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('ESP32 offline')
    expect(wrapper.text()).not.toContain('ESP32 online')
  })
})
