package fes.smartown.backend.airport.model;

public record AirportStatePayload(
        AirportMode mode,
        boolean lightsOn,
        boolean online
) {
}
