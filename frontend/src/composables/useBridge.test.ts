/*
 * KI-Hinweis:
 * Diese Testdatei wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
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

import { useBridge } from './useBridge'

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

const BridgeHarness = defineComponent({
  setup() {
    const bridge = useBridge()

    return () =>
      h('div', [
        h('span', { id: 'mode' }, bridge.snapshot.value?.mode ?? 'none'),
        h('span', { id: 'open' }, bridge.snapshot.value?.isPhysicallyOpen ? 'open' : 'closed'),
        h('span', { id: 'online' }, bridge.bridgeOnline.value ? 'online' : 'offline'),
        h('span', { id: 'live' }, bridge.liveConnected.value ? 'live' : 'offline'),
        h('button', { id: 'open-mode', onClick: () => bridge.setBridgeMode('MANUAL_OPEN') }, 'open-mode'),
        h('button', { id: 'close-mode', onClick: () => bridge.setBridgeMode('MANUAL_CLOSE') }, 'close-mode'),
      ])
  },
})

describe('useBridge', () => {
  beforeEach(() => {
    fetchMock.mockImplementation(async (input: string) => ({
      ok: true,
      json: async () => {
        if (input === '/api/bridge') {
          return {
            mode: 'AUTO',
            isPhysicallyOpen: false,
            brokerConnected: true,
            espOnline: true,
            updatedAt: '2026-04-30T09:00:00Z',
          }
        }

        return null
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

  it('opens the bridge websocket after loading the initial snapshot', async () => {
    const wrapper = mount(BridgeHarness)
    await flushPromises()

    expect(openWebSocketMock).toHaveBeenCalledWith('ws://localhost/ws/bridge')
    expect(MockWebSocket.instances.map((socket) => socket.url)).toEqual(['ws://localhost/ws/bridge'])
    expect(wrapper.get('#mode').text()).toBe('AUTO')
    expect(wrapper.get('#open').text()).toBe('closed')
    expect(wrapper.get('#online').text()).toBe('online')
  })

  it('updates the bridge snapshot from websocket messages and does not start polling', async () => {
    const setIntervalSpy = vi.spyOn(window, 'setInterval')
    const wrapper = mount(BridgeHarness)
    await flushPromises()

    expect(setIntervalSpy).not.toHaveBeenCalled()

    const bridgeSocket = MockWebSocket.instances[0]
    bridgeSocket.onmessage?.({
      data: JSON.stringify({
        mode: 'MANUAL_OPEN',
        isPhysicallyOpen: true,
        brokerConnected: true,
        espOnline: true,
        updatedAt: '2026-04-30T09:15:00Z',
      }),
    } as MessageEvent<string>)
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('MANUAL_OPEN')
    expect(wrapper.get('#open').text()).toBe('open')
    expect(wrapper.get('#online').text()).toBe('online')
  })

  it('keeps the latest selected bridge mode when requests finish out of order', async () => {
    const firstUpdate = createDeferred<{ ok: boolean }>()
    const secondUpdate = createDeferred<{ ok: boolean }>()

    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input === '/api/bridge') {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            mode: 'AUTO',
            isPhysicallyOpen: false,
            brokerConnected: true,
            espOnline: true,
            updatedAt: '2026-04-30T09:00:00Z',
          }),
        })
      }

      if (input === '/api/bridge/mode') {
        if (init?.body === JSON.stringify({ mode: 'MANUAL_OPEN' })) {
          return firstUpdate.promise
        }

        return secondUpdate.promise
      }

      throw new Error(`Unexpected fetch input: ${input}`)
    })

    const wrapper = mount(BridgeHarness)
    await flushPromises()

    await Promise.all([
      wrapper.get('#open-mode').trigger('click'),
      wrapper.get('#close-mode').trigger('click'),
    ])

    secondUpdate.resolve({ ok: true })
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('MANUAL_CLOSE')

    firstUpdate.resolve({ ok: true })
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('MANUAL_CLOSE')
  })

  it('ignores stale bridge websocket modes until the latest manual mode is confirmed', async () => {
    const firstUpdate = createDeferred<{ ok: boolean }>()
    const secondUpdate = createDeferred<{ ok: boolean }>()

    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input === '/api/bridge') {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            mode: 'AUTO',
            isPhysicallyOpen: false,
            brokerConnected: true,
            espOnline: true,
            updatedAt: '2026-04-30T09:00:00Z',
          }),
        })
      }

      if (input === '/api/bridge/mode') {
        if (init?.body === JSON.stringify({ mode: 'MANUAL_OPEN' })) {
          return firstUpdate.promise
        }

        return secondUpdate.promise
      }

      throw new Error(`Unexpected fetch input: ${input}`)
    })

    const wrapper = mount(BridgeHarness)
    await flushPromises()

    await Promise.all([
      wrapper.get('#open-mode').trigger('click'),
      wrapper.get('#close-mode').trigger('click'),
    ])

    secondUpdate.resolve({ ok: true })
    await flushPromises()

    const bridgeSocket = MockWebSocket.instances[0]

    bridgeSocket.onmessage?.({
      data: JSON.stringify({
        mode: 'MANUAL_OPEN',
        isPhysicallyOpen: true,
        brokerConnected: true,
        espOnline: true,
        updatedAt: '2026-04-30T09:15:00Z',
      }),
    } as MessageEvent<string>)
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('MANUAL_CLOSE')
    expect(wrapper.get('#open').text()).toBe('open')

    bridgeSocket.onmessage?.({
      data: JSON.stringify({
        mode: 'MANUAL_CLOSE',
        isPhysicallyOpen: false,
        brokerConnected: true,
        espOnline: true,
        updatedAt: '2026-04-30T09:15:01Z',
      }),
    } as MessageEvent<string>)
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('MANUAL_CLOSE')
    expect(wrapper.get('#open').text()).toBe('closed')

    firstUpdate.resolve({ ok: true })
    await flushPromises()

    expect(wrapper.get('#mode').text()).toBe('MANUAL_CLOSE')
  })

  it('reports live only after the websocket is actually open', async () => {
    const wrapper = mount(BridgeHarness)
    await flushPromises()

    const bridgeSocket = MockWebSocket.instances[0]

    expect(wrapper.get('#live').text()).toBe('offline')

    bridgeSocket.open()
    await flushPromises()

    expect(wrapper.get('#live').text()).toBe('live')

    bridgeSocket.close()
    await flushPromises()

    expect(wrapper.get('#live').text()).toBe('offline')
  })
})
