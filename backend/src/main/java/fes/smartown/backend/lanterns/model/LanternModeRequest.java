/**
 * KI-Hinweis:
 * Diese Klasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.model;

import jakarta.validation.constraints.NotNull;

/**
 * REST-Request fuer einen expliziten Moduswechsel.
 */
public record LanternModeRequest(
        @NotNull LanternMode mode
) {
}
