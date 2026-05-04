package fes.smartown.backend.lanterns.api;

import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternModeRequest;
import fes.smartown.backend.lanterns.model.LanternSnapshot;
import fes.smartown.backend.lanterns.service.LanternCommandPublisher;
import fes.smartown.backend.lanterns.service.LanternStateService;
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
@RequestMapping("/api/lanterns")
/**
 * Stellt Snapshot und Moduswechsel fuer die sensorbasierten Laternen per REST bereit.
 */
public class LanternController {

    private final LanternStateService lanternStateService;
    private final LanternCommandPublisher lanternCommandPublisher;

    public LanternController(LanternStateService lanternStateService,
                             LanternCommandPublisher lanternCommandPublisher) {
        this.lanternStateService = lanternStateService;
        this.lanternCommandPublisher = lanternCommandPublisher;
    }

    @GetMapping
    /**
     * Liefert den zuletzt bekannten Laternenzustand inklusive letztem Event.
     */
    public LanternSnapshot getSnapshot() {
        return lanternStateService.getSnapshot();
    }

    @PutMapping("/mode")
    @ResponseStatus(HttpStatus.ACCEPTED)
    /**
     * Stoesst einen Moduswechsel an und liefert danach den aktuellen Snapshot zurueck.
     */
    public LanternSnapshot updateMode(@Valid @RequestBody LanternModeRequest request) {
        LanternMode mode = request.mode();
        try {
            lanternCommandPublisher.publishModeCommand(mode);
            return lanternStateService.getSnapshot();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
        }
    }
}
