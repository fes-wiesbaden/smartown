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

const LanternHarness = defineComponent({
  setup() {
    const lanterns = useLanterns()

    return () =>
      h('div', [
        h('span', { id: 'mode' }, lanterns.snapshot.value?.state.mode ?? 'none'),
        h('span', { id: 'light' }, lanterns.snapshot.value?.state.lightState ?? 'none'),
        h('span', { id: 'live' }, lanterns.liveConnected.value ? 'live' : 'offline'),
        h('button', { id: 'switch-on', onClick: () => lanterns.setMode('ON') }, 'on'),
        h('button', { id: 'switch-off', onClick: () => lanterns.setMode('OFF') }, 'off'),
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

  it('keeps the latest selected mode when two mode updates finish out of order', async () => {
    const firstUpdate = createDeferred<{ ok: boolean; json: () => Promise<unknown> }>()
    const secondUpdate = createDeferred<{ ok: boolean; json: () => Promise<unknown> }>()

    fetchMock.mockImplementation((input: string) => {
      if (input === '/api/lanterns') {
        return Promise.resolve({
          ok: true,
          json: async () => ({
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
          }),
        })
      }

      if (input === '/api/lanterns/mode') {
        const body = (fetchMock.mock.calls.at(-1)?.[1] as { body?: string } | undefined)?.body
        if (body === JSON.stringify({ mode: 'ON' })) {
          return firstUpdate.promise
        }

        return secondUpdate.promise
      }

      throw new Error(`Unexpected fetch input: ${input}`)
    })

    const wrapper = mount(LanternHarness)
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

  it('ignores stale websocket modes until the latest manual mode is confirmed', async () => {
    const firstUpdate = createDeferred<{ ok: boolean; json: () => Promise<unknown> }>()
    const secondUpdate = createDeferred<{ ok: boolean; json: () => Promise<unknown> }>()

    fetchMock.mockImplementation((input: string) => {
      if (input === '/api/lanterns') {
        return Promise.resolve({
          ok: true,
          json: async () => ({
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
          }),
        })
      }

      if (input === '/api/lanterns/mode') {
        const body = (fetchMock.mock.calls.at(-1)?.[1] as { body?: string } | undefined)?.body
        if (body === JSON.stringify({ mode: 'ON' })) {
          return firstUpdate.promise
        }

        return secondUpdate.promise
      }

      throw new Error(`Unexpected fetch input: ${input}`)
    })

    const wrapper = mount(LanternHarness)
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

    expect(wrapper.get('#mode').text()).toBe('OFF')
    expect(wrapper.get('#light').text()).toBe('ON')

    lanternSocket.onmessage?.({
      data: JSON.stringify({
        state: {
          mode: 'OFF',
          lightState: 'OFF',
          lux: 12.5,
          online: true,
          thresholdLux: 50,
        },
        lastEvent: {
          type: 'MODE_CHANGED',
          lightState: 'OFF',
          reason: 'MANUAL_OVERRIDE',
        },
        brokerConnected: true,
        updatedAt: '2026-04-30T10:00:04Z',
      }),
    } as MessageEvent<string>)
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('OFF')
    expect(wrapper.get('#light').text()).toBe('OFF')

    firstUpdate.resolve({
      ok: true,
      json: async () => ({}),
    })
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('OFF')
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
