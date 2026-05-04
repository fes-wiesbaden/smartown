/**
 * KI-Hinweis:
 * Diese Klasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.model;

/**
 * MQTT-State-Payload mit allen Werten, die das Frontend anzeigen soll.
 */
public record LanternStatePayload(
        LanternMode mode,
        LightState lightState,
        Double lux,
        boolean online,
        Double thresholdLux
) {
}
