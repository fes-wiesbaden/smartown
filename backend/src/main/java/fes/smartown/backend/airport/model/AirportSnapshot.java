package fes.smartown.backend.airport.model;

import java.time.Instant;

public record AirportSnapshot(
        AirportStatePayload state,
        boolean brokerConnected,
        Instant updatedAt
) {
}
