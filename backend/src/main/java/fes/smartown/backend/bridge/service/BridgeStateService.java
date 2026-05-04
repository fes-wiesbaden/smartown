package fes.smartown.backend.bridge.service;

import fes.smartown.backend.bridge.model.BridgeMode;
import fes.smartown.backend.bridge.model.BridgeSnapshot;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
/**
 * Haelt den Brueckenzustand im Speicher und pusht Aenderungen direkt an WebSocket-Clients.
 */
public class BridgeStateService {

    static final Duration DEVICE_OFFLINE_TIMEOUT = Duration.ofSeconds(15);

    private enum BridgeState {
        IDLE, SENSOR_1_FIRST, SENSOR_2_FIRST
    }

    private BridgeState currentState = BridgeState.IDLE;
    private BridgeMode currentMode = BridgeMode.AUTO;
    private boolean isPhysicallyOpen = false;
    private boolean brokerConnected = false;
    private Instant lastDeviceMessageAt;

    private final BridgeCommandPublisher bridgeCommandPublisher;
    private final BridgeRealtimeService bridgeRealtimeService;

    public BridgeStateService(@Lazy BridgeCommandPublisher bridgeCommandPublisher,
                              BridgeRealtimeService bridgeRealtimeService) {
        this.bridgeCommandPublisher = bridgeCommandPublisher;
        this.bridgeRealtimeService = bridgeRealtimeService;
    }

    private synchronized void broadcastSnapshot() {
        bridgeRealtimeService.broadcast(getSnapshot());
    }

    /**
     * Liefert den letzten bekannten Snapshot, inklusive lazy Offline-Pruefung.
     */
    public synchronized BridgeSnapshot getSnapshot() {
        return getSnapshot(Instant.now());
    }

    synchronized BridgeSnapshot getSnapshot(Instant now) {
        Objects.requireNonNull(now, "now");
        expireStaleDeviceIfNecessary(now);
        return snapshotAt(now);
    }

    public synchronized void updateBrokerConnection(boolean connected) {
        this.brokerConnected = connected;
        broadcastSnapshot();
    }

    /**
     * State-Topic ohne Fachereignis: zaehlt nur als Lebenszeichen.
     */
    public synchronized void handleHeartbeat() {
        this.lastDeviceMessageAt = Instant.now();
        broadcastSnapshot();
    }

    synchronized void handleHeartbeat(Instant receivedAt) {
        Objects.requireNonNull(receivedAt, "receivedAt");
        this.lastDeviceMessageAt = receivedAt;
        broadcastSnapshot(receivedAt);
    }

    public synchronized void setMode(BridgeMode mode) {
        Objects.requireNonNull(mode, "mode");
        this.currentMode = mode;
        this.currentState = BridgeState.IDLE; // Reset state machine on mode change
        if (mode == BridgeMode.MANUAL_OPEN) {
            openBridgeSafely();
        } else if (mode == BridgeMode.MANUAL_CLOSE) {
            closeBridgeSafely();
        }
        broadcastSnapshot();
    }

    /**
     * Event-Topic fuer Bootsensoren im Automatikmodus.
     */
    public synchronized void handleEvent(String eventPayload) {
        handleEvent(eventPayload, Instant.now());
    }

    synchronized void handleEvent(String eventPayload, Instant receivedAt) {
        Objects.requireNonNull(eventPayload, "eventPayload");
        Objects.requireNonNull(receivedAt, "receivedAt");
        this.lastDeviceMessageAt = receivedAt;
        if (currentMode != BridgeMode.AUTO) {
            broadcastSnapshot(receivedAt);
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
        } else if (currentState == BridgeState.SENSOR_1_FIRST && "BOAT_DETECTED_SENSOR_2".equals(eventPayload)) {
            currentState = BridgeState.IDLE;
            closeBridgeSafely();
        } else if (currentState == BridgeState.SENSOR_2_FIRST && "BOAT_DETECTED_SENSOR_1".equals(eventPayload)) {
            currentState = BridgeState.IDLE;
            closeBridgeSafely();
        }
        broadcastSnapshot(receivedAt);
    }

    @Scheduled(fixedDelay = 5000)
    void expireStaleDeviceIfNecessary() {
        synchronized (this) {
            expireStaleDeviceIfNecessary(Instant.now());
        }
    }

    synchronized void expireStaleDeviceIfNecessary(Instant now) {
        Objects.requireNonNull(now, "now");
        if (lastDeviceMessageAt == null || Duration.between(lastDeviceMessageAt, now).compareTo(DEVICE_OFFLINE_TIMEOUT) < 0) {
            return;
        }

        lastDeviceMessageAt = null;
        broadcastSnapshot(now);
    }

    private boolean openBridgeSafely() {
        if (!isPhysicallyOpen) {
            bridgeCommandPublisher.publishCommand("OPEN");
            isPhysicallyOpen = true;
            return true;
        }
        return false;
    }

    private boolean closeBridgeSafely() {
        if (isPhysicallyOpen) {
            bridgeCommandPublisher.publishCommand("CLOSE");
            isPhysicallyOpen = false;
            return true;
        }
        return false;
    }

    private boolean isEspOnlineAt(Instant now) {
        return lastDeviceMessageAt != null
                && Duration.between(lastDeviceMessageAt, now).compareTo(DEVICE_OFFLINE_TIMEOUT) < 0;
    }

    /**
     * Baut den Snapshot jedes Mal frisch aus dem internen Zustand zusammen.
     */
    private BridgeSnapshot snapshotAt(Instant now) {
        return new BridgeSnapshot(currentMode, isPhysicallyOpen, brokerConnected, isEspOnlineAt(now), now);
    }

    /**
     * Kapselt den Broadcast, damit alle Aufrufer denselben Snapshot-Zeitpunkt teilen.
     */
    private void broadcastSnapshot(Instant now) {
        bridgeRealtimeService.broadcast(snapshotAt(now));
    }
}
