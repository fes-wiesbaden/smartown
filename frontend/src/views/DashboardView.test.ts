import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const fetchMock = vi.fn()
const openWebSocketMock = vi.hoisted(() => vi.fn())

vi.mock('@/composables/backendEndpoints', () => ({
  openWebSocket: openWebSocketMock,
  resolveApiBase: () => '/api',
  resolveWebSocketUrl: (path: string) => `ws://localhost${path}`,
}))

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

  close() {
    this.onclose?.()
  }
}

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
    openWebSocketMock.mockImplementation((url: string) => new MockWebSocket(url))
    MockWebSocket.instances = []
  })

  /**
   * Raeumt globale Stubs nach jedem Testlauf auf.
   */
  afterEach(() => {
    vi.unstubAllGlobals()
    openWebSocketMock.mockReset()
  })

  /**
   * Prueft, dass das Dashboard den Snapshot rendert und Moduswechsel per REST ausloest.
   */
  it('renders both live modules and sends mode updates', async () => {
    const DashboardView = await loadDashboardView()
    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('Kontrollzentrum')
    expect(wrapper.text()).toContain('Live')
    expect(wrapper.text()).toContain('Laternen')
    expect(wrapper.text()).toContain('Brücke')
    expect(wrapper.text()).toContain('ESP32 online')
    expect(wrapper.text()).toContain('12.5 lx')
    expect(wrapper.text()).toContain('UNTEN')

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
    expect(wrapper.text()).toContain('Steuerung erst moeglich, wenn Broker und ESP32 online sind.')
  })

  it('disables all control buttons when broker or device connectivity is missing', async () => {
    fetchMock.mockImplementation(async (input: string) => ({
      ok: true,
      json: async () => {
        if (input === '/api/bridge') {
          return {
            ...bridgeSnapshot,
            brokerConnected: false,
            espOnline: false,
          }
        }

        return {
          ...lanternSnapshot,
          brokerConnected: false,
          state: {
            ...lanternSnapshot.state,
            online: false,
          },
        }
      },
    }))

    const DashboardView = await loadDashboardView()
    const wrapper = mount(DashboardView)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    expect(buttons).toHaveLength(6)
    expect(buttons.every((button) => button.attributes('disabled') !== undefined)).toBe(true)
    expect(wrapper.text()).toContain('Steuerung erst moeglich, wenn Broker und ESP32 online sind.')
  })
})
