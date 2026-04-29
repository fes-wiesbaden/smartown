package fes.smartown.backend.bridge.service;

import fes.smartown.backend.bridge.model.BridgeMode;
import fes.smartown.backend.bridge.model.BridgeSnapshot;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class BridgeStateService {

    private enum BridgeState {
        IDLE, SENSOR_1_FIRST, SENSOR_2_FIRST
    }

    private BridgeState currentState = BridgeState.IDLE;
    private BridgeMode currentMode = BridgeMode.AUTO;
    private boolean isPhysicallyOpen = false;
    
    private boolean brokerConnected = false;
    private Instant lastDeviceMessageAt = Instant.MIN;

    private final BridgeCommandPublisher bridgeCommandPublisher;

    public BridgeStateService(@Lazy BridgeCommandPublisher bridgeCommandPublisher) {
        this.bridgeCommandPublisher = bridgeCommandPublisher;
    }
    
    public synchronized BridgeSnapshot getSnapshot() {
        boolean espOnline = lastDeviceMessageAt != null && 
                            Duration.between(lastDeviceMessageAt, Instant.now()).getSeconds() < 15;
        return new BridgeSnapshot(currentMode, isPhysicallyOpen, brokerConnected, espOnline, Instant.now());
    }
    
    public synchronized void updateBrokerConnection(boolean connected) {
        this.brokerConnected = connected;
    }
    
    public synchronized void handleHeartbeat() {
        this.lastDeviceMessageAt = Instant.now();
    }

    public synchronized void setMode(BridgeMode mode) {
        this.currentMode = mode;
        if (mode == BridgeMode.MANUAL_OPEN) {
            openBridgeSafely();
        } else if (mode == BridgeMode.MANUAL_CLOSE) {
            closeBridgeSafely();
        }
    }

    public synchronized void handleEvent(String eventPayload) {
        this.lastDeviceMessageAt = Instant.now(); // Jedes Event ist auch ein Lebenszeichen
        if (currentMode != BridgeMode.AUTO) return;

        if (currentState == BridgeState.IDLE) {
            if ("BOAT_DETECTED_SENSOR_1".equals(eventPayload)) {
                currentState = BridgeState.SENSOR_1_FIRST;
                openBridgeSafely();
            } else if ("BOAT_DETECTED_SENSOR_2".equals(eventPayload)) {
                currentState = BridgeState.SENSOR_2_FIRST;
                openBridgeSafely();
            }
        } else if (currentState == BridgeState.SENSOR_1_FIRST && "BOAT_DETECTED_SENSOR_2".equals(eventPayload)) {
            currentState = BridgeState.IDLE;
            closeBridgeSafely();
        } else if (currentState == BridgeState.SENSOR_2_FIRST && "BOAT_DETECTED_SENSOR_1".equals(eventPayload)) {
            currentState = BridgeState.IDLE;
            closeBridgeSafely();
        }
    }

    private void openBridgeSafely() {
        if (!isPhysicallyOpen) {
            bridgeCommandPublisher.publishCommand("OPEN");
            isPhysicallyOpen = true;
        }
    }

    private void closeBridgeSafely() {
        if (isPhysicallyOpen) {
            bridgeCommandPublisher.publishCommand("CLOSE");
            isPhysicallyOpen = false;
        }
    }
}
