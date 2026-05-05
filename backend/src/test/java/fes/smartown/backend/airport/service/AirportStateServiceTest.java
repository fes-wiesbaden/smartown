package fes.smartown.backend.airport.service;

import fes.smartown.backend.airport.model.AirportMode;
import fes.smartown.backend.airport.model.AirportSnapshot;
import fes.smartown.backend.airport.model.AirportStatePayload;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AirportStateServiceTest {

    @Test
    void mergesStateIntoCurrentSnapshot() {
        AirportRealtimeService realtimeService = mock(AirportRealtimeService.class);
        AirportStateService airportStateService = new AirportStateService(realtimeService);

        AirportSnapshot snapshot = airportStateService.handleState(
                new AirportStatePayload(AirportMode.ON, true, true),
                Instant.parse("2026-05-05T10:15:00Z")
        );

        assertThat(snapshot.state().mode()).isEqualTo(AirportMode.ON);
        assertThat(snapshot.state().lightsOn()).isTrue();
        assertThat(snapshot.state().online()).isTrue();
        verify(realtimeService).broadcast(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void marksAirportOfflineAfterHeartbeatTimeout() {
        AirportRealtimeService realtimeService = mock(AirportRealtimeService.class);
        AirportStateService airportStateService = new AirportStateService(realtimeService);
        Instant stateAt = Instant.parse("2026-05-05T10:15:00Z");

        airportStateService.handleState(new AirportStatePayload(AirportMode.ON, true, true), stateAt);
        airportStateService.expireStaleDeviceIfNecessary(stateAt.plus(AirportStateService.DEVICE_OFFLINE_TIMEOUT).plusSeconds(1));

        assertThat(airportStateService.getSnapshot().state().online()).isFalse();
        verify(realtimeService, times(2)).broadcast(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void keepsAirportOfflineForFirstStateAfterBrokerReconnect() {
        AirportRealtimeService realtimeService = mock(AirportRealtimeService.class);
        AirportStateService airportStateService = new AirportStateService(realtimeService);

        airportStateService.updateBrokerConnection(true, Instant.parse("2026-05-05T10:15:00Z"));

        AirportSnapshot snapshot = airportStateService.handleState(
                new AirportStatePayload(AirportMode.ON, true, true),
                Instant.parse("2026-05-05T10:15:01Z")
        );

        assertThat(snapshot.state().online()).isFalse();
        assertThat(snapshot.state().lightsOn()).isTrue();
        verify(realtimeService, times(2)).broadcast(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void marksAirportOnlineForSecondStateAfterBrokerReconnect() {
        AirportRealtimeService realtimeService = mock(AirportRealtimeService.class);
        AirportStateService airportStateService = new AirportStateService(realtimeService);

        airportStateService.updateBrokerConnection(true, Instant.parse("2026-05-05T10:15:00Z"));
        airportStateService.handleState(
                new AirportStatePayload(AirportMode.OFF, false, true),
                Instant.parse("2026-05-05T10:15:01Z")
        );

        AirportSnapshot snapshot = airportStateService.handleState(
                new AirportStatePayload(AirportMode.ON, true, true),
                Instant.parse("2026-05-05T10:15:05Z")
        );

        assertThat(snapshot.state().online()).isTrue();
        assertThat(snapshot.state().mode()).isEqualTo(AirportMode.ON);
        verify(realtimeService, times(3)).broadcast(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void broadcastsBrokerReconnectTransition() {
        AirportRealtimeService realtimeService = mock(AirportRealtimeService.class);
        AirportStateService airportStateService = new AirportStateService(realtimeService);
        Instant firstStateAt = Instant.parse("2026-05-05T10:15:00Z");

        airportStateService.handleState(new AirportStatePayload(AirportMode.ON, true, true), firstStateAt);

        AirportSnapshot snapshot = airportStateService.updateBrokerConnection(true, firstStateAt.plusSeconds(10));

        assertThat(snapshot.state().online()).isFalse();
        assertThat(snapshot.brokerConnected()).isTrue();
        ArgumentCaptor<AirportSnapshot> captor = ArgumentCaptor.forClass(AirportSnapshot.class);
        verify(realtimeService, times(2)).broadcast(captor.capture());
        assertThat(captor.getAllValues().getLast().brokerConnected()).isTrue();
    }
}
