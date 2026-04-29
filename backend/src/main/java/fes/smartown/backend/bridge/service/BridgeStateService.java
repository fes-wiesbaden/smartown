package fes.smartown.backend.bridge.service;

import fes.smartown.backend.bridge.model.BridgeMode;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class BridgeStateService {

    private enum BridgeState {
        IDLE, SENSOR_1_FIRST, SENSOR_2_FIRST
    }

    private BridgeState currentState = BridgeState.IDLE;
    private BridgeMode currentMode = BridgeMode.AUTO;
    
    // Verhindert, dass die Bruecke zweimal hoch- oder runtergefahren wird
    private boolean isPhysicallyOpen = false;

    private final BridgeCommandPublisher bridgeCommandPublisher;

    public BridgeStateService(@Lazy BridgeCommandPublisher bridgeCommandPublisher) {
        this.bridgeCommandPublisher = bridgeCommandPublisher;
    }

    public synchronized void setMode(BridgeMode mode) {
        this.currentMode = mode;
        if (mode == BridgeMode.MANUAL_OPEN) {
            openBridgeSafely();
        } else if (mode == BridgeMode.MANUAL_CLOSE) {
            closeBridgeSafely();
        } else if (mode == BridgeMode.AUTO) {
            System.out.println("[Bridge] Modus auf AUTO gesetzt");
        }
    }

    public synchronized void handleEvent(String eventPayload) {
        if (currentMode != BridgeMode.AUTO) {
            System.out.println("[Bridge] Ignoriere Sensor (Modus ist MANUELL)");
            return;
        }

        if (currentState == BridgeState.IDLE) {
            if ("BOAT_DETECTED_SENSOR_1".equals(eventPayload)) {
                currentState = BridgeState.SENSOR_1_FIRST;
                openBridgeSafely();
            } else if ("BOAT_DETECTED_SENSOR_2".equals(eventPayload)) {
                currentState = BridgeState.SENSOR_2_FIRST;
                openBridgeSafely();
            }
        } else if (currentState == BridgeState.SENSOR_1_FIRST) {
            if ("BOAT_DETECTED_SENSOR_2".equals(eventPayload)) {
                currentState = BridgeState.IDLE;
                closeBridgeSafely();
            }
        } else if (currentState == BridgeState.SENSOR_2_FIRST) {
            if ("BOAT_DETECTED_SENSOR_1".equals(eventPayload)) {
                currentState = BridgeState.IDLE;
                closeBridgeSafely();
            }
        }
    }

    private void openBridgeSafely() {
        if (!isPhysicallyOpen) {
            System.out.println("[Bridge] Sicherer Befehl: OPEN");
            bridgeCommandPublisher.publishCommand("OPEN");
            isPhysicallyOpen = true;
        } else {
            System.out.println("[Bridge] Warnung: Brücke ist bereits offen. OPEN blockiert!");
        }
    }

    private void closeBridgeSafely() {
        if (isPhysicallyOpen) {
            System.out.println("[Bridge] Sicherer Befehl: CLOSE");
            bridgeCommandPublisher.publishCommand("CLOSE");
            isPhysicallyOpen = false;
        } else {
            System.out.println("[Bridge] Warnung: Brücke ist bereits zu. CLOSE blockiert!");
        }
    }
}
