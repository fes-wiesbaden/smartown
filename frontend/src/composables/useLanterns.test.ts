import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const fetchMock = vi.fn()
const openWebSocketMock = vi.hoisted(() => vi.fn())

vi.mock('@/composables/backendEndpoints', () => ({
  openWebSocket: openWebSocketMock,
  resolveApiBase: () => '/api',
  resolveWebSocketUrl: (path: string) => `ws://localhost${path}`,
}))

import { useLanterns } from './useLanterns'

class MockWebSocket {
  static instances: MockWebSocket[] = []

  url: string
  onopen: (() => void) | null = null
  onmessage: ((event: MessageEvent<string>) => void) | null = null
  onerror: (() => void) | null = null
  onclose: (() => void) | null = null
  readyState = 0

  constructor(url: string) {
    this.url = url
    MockWebSocket.instances.push(this)
  }

  open() {
    this.readyState = 1
    this.onopen?.()
  }

  close() {
    this.readyState = 3
    this.onclose?.()
  }
}

const LanternHarness = defineComponent({
  setup() {
    const lanterns = useLanterns()

    return () =>
      h('div', [
        h('span', { id: 'mode' }, lanterns.snapshot.value?.state.mode ?? 'none'),
        h('span', { id: 'light' }, lanterns.snapshot.value?.state.lightState ?? 'none'),
        h('span', { id: 'live' }, lanterns.liveConnected.value ? 'live' : 'offline'),
        h('button', { id: 'switch-on', onClick: () => lanterns.setMode('ON') }, 'on'),
      ])
  },
})

describe('useLanterns', () => {
  beforeEach(() => {
    fetchMock.mockImplementation(async (input: string) => ({
      ok: true,
      json: async () => {
        if (input === '/api/lanterns') {
          return {
            state: {
              mode: 'AUTO',
              lightState: 'OFF',
              lux: 60,
              online: true,
              thresholdLux: 50,
            },
            lastEvent: {
              type: 'SYSTEM_START',
              lightState: 'OFF',
              reason: 'SYSTEM_START',
            },
            brokerConnected: true,
            updatedAt: '2026-04-30T10:00:00Z',
          }
        }

        return {
          state: {
            mode: 'AUTO',
            lightState: 'OFF',
            lux: 60,
            online: false,
            thresholdLux: 50,
          },
          lastEvent: {
            type: 'SYSTEM_START',
            lightState: 'OFF',
            reason: 'SYSTEM_START',
          },
          brokerConnected: true,
          updatedAt: '2026-04-30T10:00:01Z',
        }
      },
    }))

    vi.stubGlobal('fetch', fetchMock)
    openWebSocketMock.mockImplementation((url: string) => new MockWebSocket(url))
    MockWebSocket.instances = []
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    openWebSocketMock.mockReset()
  })

  it('keeps the selected mode instead of overwriting it with a stale REST snapshot', async () => {
    const wrapper = mount(LanternHarness)
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('AUTO')

    await wrapper.get('#switch-on').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/lanterns/mode',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ mode: 'ON' }),
      }),
    )
    expect(wrapper.get('#mode').text()).toBe('ON')
  })

  it('still accepts the authoritative websocket snapshot after a manual mode change', async () => {
    const wrapper = mount(LanternHarness)
    await flushPromises()

    await wrapper.get('#switch-on').trigger('click')
    await flushPromises()

    const lanternSocket = MockWebSocket.instances[0]
    lanternSocket.onmessage?.({
      data: JSON.stringify({
        state: {
          mode: 'ON',
          lightState: 'ON',
          lux: 12.5,
          online: true,
          thresholdLux: 50,
        },
        lastEvent: {
          type: 'MODE_CHANGED',
          lightState: 'ON',
          reason: 'MANUAL_OVERRIDE',
        },
        brokerConnected: true,
        updatedAt: '2026-04-30T10:00:03Z',
      }),
    } as MessageEvent<string>)
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('ON')
    expect(wrapper.get('#light').text()).toBe('ON')
  })

  it('reports live only after the websocket is actually open', async () => {
    const wrapper = mount(LanternHarness)
    await flushPromises()

    const lanternSocket = MockWebSocket.instances[0]

    expect(wrapper.get('#live').text()).toBe('offline')

    lanternSocket.open()
    await flushPromises()

    expect(wrapper.get('#live').text()).toBe('live')

    lanternSocket.close()
    await flushPromises()

    expect(wrapper.get('#live').text()).toBe('offline')
  })
})
