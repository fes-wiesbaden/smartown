package fes.smartown.backend.lanterns.model;

import java.time.Instant;

/**
 * Beschreibt einen einzelnen Lux-Messpunkt fuer den Zeitverlauf im Frontend.
 */
public record LanternLuxHistoryPoint(
        Instant measuredAt,
        double lux
) {
}
