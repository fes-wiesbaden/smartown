package fes.smartown.backend.airport.model;

public record AirportCommandPayload(
        String action,
        AirportMode mode
) {
}
