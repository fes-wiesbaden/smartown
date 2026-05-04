/**
 * KI-Hinweis:
 * Diese Klasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.model;

/**
 * Fachliche Gruende fuer einen Lichtzustandswechsel.
 */
public enum LanternReason {
    LOW_LUX,
    HIGH_LUX,
    MANUAL_OVERRIDE,
    SYSTEM_START
}
