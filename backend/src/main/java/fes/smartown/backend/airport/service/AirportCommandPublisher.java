package fes.smartown.backend.airport.service;

import fes.smartown.backend.airport.model.AirportMode;

public interface AirportCommandPublisher {

    void publishModeCommand(AirportMode mode);
}
