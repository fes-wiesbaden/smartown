package fes.smartown.backend.lanterns.service;

import fes.smartown.backend.lanterns.model.LanternEventPayload;
import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternReason;
import fes.smartown.backend.lanterns.model.LanternSnapshot;
import fes.smartown.backend.lanterns.model.LanternStatePayload;
import fes.smartown.backend.lanterns.model.LightState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
/**
 * Haltet den zuletzt bekannten MQTT-Zustand der Laternen thread-sicher im Speicher.
 */
public class LanternStateService {

    static final Duration DEVICE_OFFLINE_TIMEOUT = Duration.ofSeconds(30);
    static final double DEFAULT_THRESHOLD_LUX = 50.0;

    private final LanternRealtimeService lanternRealtimeService;
    private final AtomicReference<LanternSnapshot> snapshotReference = new AtomicReference<>(defaultSnapshot());
    private final AtomicReference<Instant> lastDeviceMessageAt = new AtomicReference<>();

    public LanternStateService(LanternRealtimeService lanternRealtimeService) {
        this.lanternRealtimeService = lanternRealtimeService;
    }

    /**
     * Liefert jederzeit den zuletzt bekannten Snapshot fuer REST und WebSocket.
     */
    public LanternSnapshot getSnapshot() {
        expireStaleDeviceIfNecessary(Instant.now());
        return snapshotReference.get();
    }

    /**
     * Uebernimmt ein eingehendes State-Payload und erzeugt daraus einen neuen Snapshot.
     */
    public LanternSnapshot handleState(LanternStatePayload statePayload) {
        return handleState(statePayload, Instant.now());
    }

    LanternSnapshot handleState(LanternStatePayload statePayload, Instant receivedAt) {
        Objects.requireNonNull(statePayload, "statePayload");
        Objects.requireNonNull(receivedAt, "receivedAt");
        lastDeviceMessageAt.set(receivedAt);
        return updateSnapshot(previous -> new LanternSnapshot(
                statePayload,
                previous.lastEvent(),
                previous.brokerConnected(),
                receivedAt
        ));
    }

    /**
     * Uebernimmt ein eingehendes Event-Payload und aktualisiert nur den Event-Teil des Snapshots.
     */
    public LanternSnapshot handleEvent(LanternEventPayload eventPayload) {
        return handleEvent(eventPayload, Instant.now());
    }

    LanternSnapshot handleEvent(LanternEventPayload eventPayload, Instant receivedAt) {
        Objects.requireNonNull(eventPayload, "eventPayload");
        Objects.requireNonNull(receivedAt, "receivedAt");
        lastDeviceMessageAt.set(receivedAt);
        return updateSnapshot(previous -> new LanternSnapshot(
                previous.state(),
                eventPayload,
                previous.brokerConnected(),
                receivedAt
        ));
    }

    /**
     * Markiert, ob das Backend aktuell mit dem MQTT-Broker verbunden ist.
     */
    public LanternSnapshot updateBrokerConnection(boolean brokerConnected) {
        return updateSnapshot(previous -> new LanternSnapshot(
                previous.state(),
                previous.lastEvent(),
                brokerConnected,
                Instant.now()
        ));
    }

    /**
     * Markiert einen stillen ESP32 nach Ablauf des Heartbeat-Timeouts als offline.
     */
    @Scheduled(fixedDelay = 15000)
    void expireStaleDeviceIfNecessary() {
        expireStaleDeviceIfNecessary(Instant.now());
    }

    void expireStaleDeviceIfNecessary(Instant now) {
        Objects.requireNonNull(now, "now");

        Instant lastSeenAt = lastDeviceMessageAt.get();
        if (lastSeenAt == null || Duration.between(lastSeenAt, now).compareTo(DEVICE_OFFLINE_TIMEOUT) < 0) {
            return;
        }

        updateSnapshot(previous -> {
            if (!previous.state().online()) {
                return previous;
            }

            lastDeviceMessageAt.compareAndSet(lastSeenAt, null);
            return new LanternSnapshot(
                    withOnline(previous.state(), false),
                    previous.lastEvent(),
                    previous.brokerConnected(),
                    now
            );
        });
    }

    /**
     * Aktualisiert den Snapshot atomar und pusht nur echte Aenderungen direkt an das Frontend.
     */
    private LanternSnapshot updateSnapshot(java.util.function.UnaryOperator<LanternSnapshot> updater) {
        AtomicReference<LanternSnapshot> changedSnapshot = new AtomicReference<>();
        LanternSnapshot updated = snapshotReference.updateAndGet(previous -> {
            LanternSnapshot next = updater.apply(previous);
            if (!next.equals(previous)) {
                changedSnapshot.set(next);
            }
            return next;
        });

        LanternSnapshot snapshotToBroadcast = changedSnapshot.get();
        if (snapshotToBroadcast != null) {
            lanternRealtimeService.broadcast(snapshotToBroadcast);
        }
        return updated;
    }

    private static LanternStatePayload withOnline(LanternStatePayload state, boolean online) {
        return new LanternStatePayload(
                state.mode(),
                state.lightState(),
                state.lux(),
                online,
                state.thresholdLux()
        );
    }

    /**
     * Baut den Initialzustand fuer Start, Tests und Broker-Ausfaelle.
     */
    private static LanternSnapshot defaultSnapshot() {
        LanternStatePayload defaultState = new LanternStatePayload(
                LanternMode.AUTO,
                LightState.OFF,
                null,
                false,
                DEFAULT_THRESHOLD_LUX
        );
        LanternEventPayload defaultEvent = new LanternEventPayload(
                "SYSTEM_START",
                LightState.OFF,
                LanternReason.SYSTEM_START
        );

        return new LanternSnapshot(defaultState, defaultEvent, false, Instant.now());
    }
}
