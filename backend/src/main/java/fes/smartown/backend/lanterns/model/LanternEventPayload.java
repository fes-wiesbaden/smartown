/**
 * KI-Hinweis:
 * Diese Klasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.model;

/**
 * MQTT-Event vom ESP32 an Backend und Frontend.
 */
public record LanternEventPayload(
        String type,
        LightState lightState,
        LanternReason reason
) {
}
