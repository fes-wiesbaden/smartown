package fes.smartown.backend.bridge.api;

import fes.smartown.backend.bridge.model.BridgeMode;
import fes.smartown.backend.bridge.model.BridgeSnapshot;
import fes.smartown.backend.bridge.service.BridgeStateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bridge")
/**
 * Stellt Snapshot und Moduswechsel fuer das Brueckenmodul per REST bereit.
 */
public class BridgeController {

    private final BridgeStateService bridgeStateService;

    public BridgeController(BridgeStateService bridgeStateService) {
        this.bridgeStateService = bridgeStateService;
    }

    @GetMapping
    /**
     * Liefert den letzten bekannten Brueckenzustand.
     */
    public BridgeSnapshot getSnapshot() {
        return bridgeStateService.getSnapshot();
    }

    @PostMapping("/mode")
    @ResponseStatus(HttpStatus.ACCEPTED)
    /**
     * Stoesst einen manuellen Moduswechsel an.
     */
    public void setMode(@RequestBody BridgeModeRequest request) {
        try {
            bridgeStateService.setMode(request.mode());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
        }
    }

    public record BridgeModeRequest(BridgeMode mode) {}
}
