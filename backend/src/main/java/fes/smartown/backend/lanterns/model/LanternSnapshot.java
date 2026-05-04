/**
 * KI-Hinweis:
 * Diese Klasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.model;

import java.time.Instant;

/**
 * Kombiniert aktuellen Zustand, letztes Event und Broker-Status fuer REST und WebSocket.
 */
public record LanternSnapshot(
        LanternStatePayload state,
        LanternEventPayload lastEvent,
        boolean brokerConnected,
        Instant updatedAt
) {
}
