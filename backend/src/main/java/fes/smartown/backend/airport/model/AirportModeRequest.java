package fes.smartown.backend.airport.model;

import jakarta.validation.constraints.NotNull;

public record AirportModeRequest(
        @NotNull AirportMode mode
) {
}
