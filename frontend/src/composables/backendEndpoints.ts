const DEV_SERVER_PORT = '5173'
const BACKEND_PORT = '8080'

/**
 * Nutzt im Dev-Server direkt das Backend, im Container den Reverse Proxy.
 */
export function resolveApiBase(): string {
  if (window.location.port === DEV_SERVER_PORT) {
    return `${window.location.protocol}//${window.location.hostname}:${BACKEND_PORT}/api`
  }

  return '/api'
}

/**
 * Baut fuer jedes Modul die passende WebSocket-URL aus der aktuellen Laufzeitumgebung.
 */
export function resolveWebSocketUrl(path: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const normalizedPath = path.startsWith('/') ? path : `/${path}`

  if (window.location.port === DEV_SERVER_PORT) {
    return `${protocol}://${window.location.hostname}:${BACKEND_PORT}${normalizedPath}`
  }

  return `${protocol}://${window.location.host}${normalizedPath}`
}

/**
 * Kleine Indirektion, damit Socket-Aufbau in Tests sauber mockbar bleibt.
 */
export function openWebSocket(url: string): WebSocket {
  return new WebSocket(url)
}
