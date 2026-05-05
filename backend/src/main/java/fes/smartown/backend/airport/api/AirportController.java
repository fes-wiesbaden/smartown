package fes.smartown.backend.airport.api;

import fes.smartown.backend.airport.model.AirportModeRequest;
import fes.smartown.backend.airport.model.AirportSnapshot;
import fes.smartown.backend.airport.service.AirportCommandPublisher;
import fes.smartown.backend.airport.service.AirportStateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/airport")
public class AirportController {

    private final AirportStateService airportStateService;
    private final AirportCommandPublisher airportCommandPublisher;

    public AirportController(AirportStateService airportStateService,
                             AirportCommandPublisher airportCommandPublisher) {
        this.airportStateService = airportStateService;
        this.airportCommandPublisher = airportCommandPublisher;
    }

    @GetMapping
    public AirportSnapshot getSnapshot() {
        return airportStateService.getSnapshot();
    }

    @PutMapping("/mode")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AirportSnapshot updateMode(@Valid @RequestBody AirportModeRequest request) {
        try {
            airportCommandPublisher.publishModeCommand(request.mode());
            return airportStateService.getSnapshot();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
        }
    }
}
