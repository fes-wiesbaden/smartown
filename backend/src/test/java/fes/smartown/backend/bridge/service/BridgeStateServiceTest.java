package fes.smartown.backend.bridge.service;

import fes.smartown.backend.bridge.model.BridgeMode;
import fes.smartown.backend.bridge.model.BridgeSnapshot;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BridgeStateServiceTest {

    @Test
    void broadcastsSnapshotWhenAutoEventOpensBridge() {
        BridgeCommandPublisher commandPublisher = mock(BridgeCommandPublisher.class);
        BridgeRealtimeService realtimeService = mock(BridgeRealtimeService.class);
        BridgeStateService bridgeStateService = new BridgeStateService(commandPublisher, realtimeService);
        Instant receivedAt = Instant.parse("2026-04-30T10:15:00Z");

        bridgeStateService.handleEvent("BOAT_DETECTED_SENSOR_1", receivedAt);

        verify(commandPublisher).publishCommand("OPEN");
        ArgumentCaptor<BridgeSnapshot> captor = ArgumentCaptor.forClass(BridgeSnapshot.class);
        verify(realtimeService).broadcast(captor.capture());
        assertThat(captor.getValue().mode()).isEqualTo(BridgeMode.AUTO);
        assertThat(captor.getValue().isPhysicallyOpen()).isTrue();
        assertThat(captor.getValue().espOnline()).isTrue();
    }

    @Test
    void marksBridgeOfflineAfterHeartbeatTimeoutAndBroadcastsChange() {
        BridgeCommandPublisher commandPublisher = mock(BridgeCommandPublisher.class);
        BridgeRealtimeService realtimeService = mock(BridgeRealtimeService.class);
        BridgeStateService bridgeStateService = new BridgeStateService(commandPublisher, realtimeService);
        Instant heartbeatAt = Instant.parse("2026-04-30T10:15:00Z");

        bridgeStateService.handleHeartbeat(heartbeatAt);
        bridgeStateService.expireStaleDeviceIfNecessary(heartbeatAt.plus(BridgeStateService.DEVICE_OFFLINE_TIMEOUT).plusSeconds(1));

        assertThat(bridgeStateService.getSnapshot(heartbeatAt.plus(BridgeStateService.DEVICE_OFFLINE_TIMEOUT).plusSeconds(1)).espOnline())
                .isFalse();
        ArgumentCaptor<BridgeSnapshot> captor = ArgumentCaptor.forClass(BridgeSnapshot.class);
        verify(realtimeService, times(2)).broadcast(captor.capture());
        assertThat(captor.getAllValues().getFirst().espOnline()).isTrue();
        assertThat(captor.getAllValues().getLast().espOnline()).isFalse();
    }
}
