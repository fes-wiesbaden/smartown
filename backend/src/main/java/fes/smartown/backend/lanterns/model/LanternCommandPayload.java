/**
 * KI-Hinweis:
 * Diese Klasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.model;

/**
 * MQTT-Command vom Backend an die Firmware.
 */
public record LanternCommandPayload(
        String action,
        LanternMode mode
) {
}
