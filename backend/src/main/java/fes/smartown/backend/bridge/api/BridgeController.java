package fes.smartown.backend.bridge.api;

import fes.smartown.backend.bridge.model.BridgeMode;
import fes.smartown.backend.bridge.model.BridgeSnapshot;
import fes.smartown.backend.bridge.service.BridgeStateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bridge")
@CrossOrigin(origins = "*")
public class BridgeController {

    private final BridgeStateService bridgeStateService;

    public BridgeController(BridgeStateService bridgeStateService) {
        this.bridgeStateService = bridgeStateService;
    }

    @GetMapping
    public BridgeSnapshot getSnapshot() {
        return bridgeStateService.getSnapshot();
    }

    @PostMapping("/mode")
    public void setMode(@RequestBody BridgeModeRequest request) {
        bridgeStateService.setMode(request.mode());
    }

    public record BridgeModeRequest(BridgeMode mode) {}
}
