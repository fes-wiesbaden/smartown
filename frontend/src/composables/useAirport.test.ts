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

import { useAirport } from './useAirport'

function createDeferred<T>() {
  let resolve!: (value: T | PromiseLike<T>) => void
  const promise = new Promise<T>((innerResolve) => {
    resolve = innerResolve
  })

  return { promise, resolve }
}

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

const AirportHarness = defineComponent({
  setup() {
    const airport = useAirport()

    return () =>
      h('div', [
        h('span', { id: 'mode' }, airport.snapshot.value?.state.mode ?? 'none'),
        h('span', { id: 'lights' }, airport.snapshot.value?.state.lightsOn ? 'on' : 'off'),
        h('span', { id: 'live' }, airport.liveConnected.value ? 'live' : 'offline'),
        h('button', { id: 'switch-on', onClick: () => airport.setMode('ON') }, 'on'),
        h('button', { id: 'switch-off', onClick: () => airport.setMode('OFF') }, 'off'),
      ])
  },
})

describe('useAirport', () => {
  beforeEach(() => {
    fetchMock.mockImplementation(async (input: string) => ({
      ok: true,
      json: async () => {
        if (input === '/api/airport') {
          return {
            state: {
              mode: 'OFF',
              lightsOn: false,
              online: true,
            },
            brokerConnected: true,
            updatedAt: '2026-05-05T10:00:00Z',
          }
        }

        return {}
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
    const wrapper = mount(AirportHarness)
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('OFF')

    await wrapper.get('#switch-on').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/airport/mode',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ mode: 'ON' }),
      }),
    )
    expect(wrapper.get('#mode').text()).toBe('ON')
  })

  it('still accepts the authoritative websocket snapshot after a manual mode change', async () => {
    const wrapper = mount(AirportHarness)
    await flushPromises()

    await wrapper.get('#switch-on').trigger('click')
    await flushPromises()

    const airportSocket = MockWebSocket.instances[0]
    airportSocket.onmessage?.({
      data: JSON.stringify({
        state: {
          mode: 'ON',
          lightsOn: true,
          online: true,
        },
        brokerConnected: true,
        updatedAt: '2026-05-05T10:00:03Z',
      }),
    } as MessageEvent<string>)
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('ON')
    expect(wrapper.get('#lights').text()).toBe('on')
  })

  it('keeps the latest selected mode when two mode updates finish out of order', async () => {
    const firstUpdate = createDeferred<{ ok: boolean; json: () => Promise<unknown> }>()
    const secondUpdate = createDeferred<{ ok: boolean; json: () => Promise<unknown> }>()

    fetchMock.mockImplementation((input: string) => {
      if (input === '/api/airport') {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            state: {
              mode: 'OFF',
              lightsOn: false,
              online: true,
            },
            brokerConnected: true,
            updatedAt: '2026-05-05T10:00:00Z',
          }),
        })
      }

      if (input === '/api/airport/mode') {
        const body = (fetchMock.mock.calls.at(-1)?.[1] as { body?: string } | undefined)?.body
        if (body === JSON.stringify({ mode: 'ON' })) {
          return firstUpdate.promise
        }

        return secondUpdate.promise
      }

      throw new Error(`Unexpected fetch input: ${input}`)
    })

    const wrapper = mount(AirportHarness)
    await flushPromises()

    await Promise.all([
      wrapper.get('#switch-on').trigger('click'),
      wrapper.get('#switch-off').trigger('click'),
    ])

    secondUpdate.resolve({
      ok: true,
      json: async () => ({}),
    })
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('OFF')

    firstUpdate.resolve({
      ok: true,
      json: async () => ({}),
    })
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('OFF')
  })

  it('reports live only after the websocket is actually open', async () => {
    const wrapper = mount(AirportHarness)
    await flushPromises()

    const airportSocket = MockWebSocket.instances[0]

    expect(wrapper.get('#live').text()).toBe('offline')

    airportSocket.open()
    await flushPromises()

    expect(wrapper.get('#live').text()).toBe('live')

    airportSocket.close()
    await flushPromises()

    expect(wrapper.get('#live').text()).toBe('offline')
  })
})
