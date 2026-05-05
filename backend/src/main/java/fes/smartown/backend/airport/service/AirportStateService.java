package fes.smartown.backend.airport.service;

import fes.smartown.backend.airport.model.AirportMode;
import fes.smartown.backend.airport.model.AirportSnapshot;
import fes.smartown.backend.airport.model.AirportStatePayload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AirportStateService {

    static final Duration DEVICE_OFFLINE_TIMEOUT = Duration.ofSeconds(15);

    private final AirportRealtimeService airportRealtimeService;
    private final AtomicReference<AirportSnapshot> snapshotReference = new AtomicReference<>(defaultSnapshot());
    private final AtomicReference<Instant> lastDeviceMessageAt = new AtomicReference<>();
    private final AtomicBoolean awaitingFreshDeviceMessageAfterBrokerConnect = new AtomicBoolean(false);

    public AirportStateService(AirportRealtimeService airportRealtimeService) {
        this.airportRealtimeService = airportRealtimeService;
    }

    public AirportSnapshot getSnapshot() {
        expireStaleDeviceIfNecessary(Instant.now());
        return snapshotReference.get();
    }

    public AirportSnapshot handleState(AirportStatePayload statePayload) {
        return handleState(statePayload, Instant.now());
    }

    AirportSnapshot handleState(AirportStatePayload statePayload, Instant receivedAt) {
        Objects.requireNonNull(statePayload, "statePayload");
        Objects.requireNonNull(receivedAt, "receivedAt");

        if (awaitingFreshDeviceMessageAfterBrokerConnect.compareAndSet(true, false)) {
            return updateSnapshot(previous -> new AirportSnapshot(
                    withOnline(statePayload, false),
                    previous.brokerConnected(),
                    receivedAt
            ));
        }

        lastDeviceMessageAt.set(receivedAt);
        return updateSnapshot(previous -> new AirportSnapshot(
                withOnline(statePayload, isDeviceOnlineAt(receivedAt)),
                previous.brokerConnected(),
                receivedAt
        ));
    }

    public AirportSnapshot updateBrokerConnection(boolean brokerConnected) {
        return updateBrokerConnection(brokerConnected, Instant.now());
    }

    AirportSnapshot updateBrokerConnection(boolean brokerConnected, Instant updatedAt) {
        boolean wasBrokerConnected = snapshotReference.get().brokerConnected();
        boolean awaitingFreshDeviceMessage = brokerConnected && !wasBrokerConnected;

        if (awaitingFreshDeviceMessage) {
            awaitingFreshDeviceMessageAfterBrokerConnect.set(true);
            lastDeviceMessageAt.set(null);
        } else if (!brokerConnected) {
            awaitingFreshDeviceMessageAfterBrokerConnect.set(false);
            lastDeviceMessageAt.set(null);
        }

        return updateSnapshot(previous -> new AirportSnapshot(
                withOnline(previous.state(), brokerConnected
                        && !awaitingFreshDeviceMessage
                        && isDeviceOnlineAt(updatedAt)),
                brokerConnected,
                updatedAt
        ));
    }

    @Scheduled(fixedDelay = 5000)
    void expireStaleDeviceIfNecessary() {
        expireStaleDeviceIfNecessary(Instant.now());
    }

    void expireStaleDeviceIfNecessary(Instant now) {
        Objects.requireNonNull(now, "now");

        Instant lastSeenAt = lastDeviceMessageAt.get();
        if (lastSeenAt == null || Duration.between(lastSeenAt, now).compareTo(DEVICE_OFFLINE_TIMEOUT) < 0) {
            return;
        }

        if (!lastDeviceMessageAt.compareAndSet(lastSeenAt, null)) {
            return;
        }

        updateSnapshot(previous -> {
            if (!previous.state().online()) {
                return previous;
            }

            return new AirportSnapshot(
                    withOnline(previous.state(), false),
                    previous.brokerConnected(),
                    now
            );
        });
    }

    private AirportSnapshot updateSnapshot(java.util.function.UnaryOperator<AirportSnapshot> updater) {
        AtomicReference<AirportSnapshot> changedSnapshot = new AtomicReference<>();
        AirportSnapshot updated = snapshotReference.updateAndGet(previous -> {
            AirportSnapshot next = updater.apply(previous);
            if (!next.equals(previous)) {
                changedSnapshot.set(next);
            }
            return next;
        });

        AirportSnapshot snapshotToBroadcast = changedSnapshot.get();
        if (snapshotToBroadcast != null) {
            airportRealtimeService.broadcast(snapshotToBroadcast);
        }
        return updated;
    }

    private static AirportStatePayload withOnline(AirportStatePayload state, boolean online) {
        return new AirportStatePayload(state.mode(), state.lightsOn(), online);
    }

    private boolean isDeviceOnlineAt(Instant now) {
        Instant lastSeenAt = lastDeviceMessageAt.get();
        return lastSeenAt != null
                && Duration.between(lastSeenAt, now).compareTo(DEVICE_OFFLINE_TIMEOUT) < 0;
    }

    private static AirportSnapshot defaultSnapshot() {
        return new AirportSnapshot(
                new AirportStatePayload(AirportMode.OFF, false, false),
                false,
                Instant.now()
        );
    }
}
