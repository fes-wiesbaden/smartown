/**
 * KI-Hinweis:
 * Diese Testklasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.service;

import fes.smartown.backend.lanterns.model.LanternEventPayload;
import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternReason;
import fes.smartown.backend.lanterns.model.LanternStatePayload;
import fes.smartown.backend.lanterns.model.LightState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Prueft das Zusammenfuehren von State- und Event-Daten im In-Memory-Snapshot.
 */
class LanternStateServiceTest {

    @Test
    /**
     * Erwartet, dass auch reine Event-Nachrichten die Laterne als online markieren.
     */
    void marksLanternOnlineWhenOnlyAnEventArrives() {
        LanternRealtimeService realtimeService = mock(LanternRealtimeService.class);
        LanternStateService lanternStateService = new LanternStateService(realtimeService);

        var snapshot = lanternStateService.handleEvent(new LanternEventPayload(
                "LIGHT_STATE_CHANGED",
                LightState.ON,
                LanternReason.MANUAL_OVERRIDE
        ), Instant.parse("2026-04-29T10:15:00Z"));

        assertThat(snapshot.state().online()).isTrue();
        verify(realtimeService).broadcast(org.mockito.ArgumentMatchers.any());
    }

    @Test
    /**
     * Erwartet, dass State und Event den Snapshot aktualisieren und einen Broadcast ausloesen.
     */
    void mergesStateAndEventPayloadsIntoCurrentSnapshot() {
        LanternRealtimeService realtimeService = mock(LanternRealtimeService.class);
        LanternStateService lanternStateService = new LanternStateService(realtimeService);

        lanternStateService.handleState(new LanternStatePayload(
                LanternMode.AUTO,
                LightState.ON,
                14.5,
                true,
                50.0
        ));
        lanternStateService.handleEvent(new LanternEventPayload(
                "LIGHT_STATE_CHANGED",
                LightState.ON,
                LanternReason.LOW_LUX
        ));

        assertThat(lanternStateService.getSnapshot().state().lightState()).isEqualTo(LightState.ON);
        assertThat(lanternStateService.getSnapshot().state().lux()).isEqualTo(14.5);
        assertThat(lanternStateService.getSnapshot().lastEvent().reason()).isEqualTo(LanternReason.LOW_LUX);
        ArgumentCaptor<fes.smartown.backend.lanterns.model.LanternSnapshot> captor =
                ArgumentCaptor.forClass(fes.smartown.backend.lanterns.model.LanternSnapshot.class);
        verify(realtimeService, times(2)).broadcast(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().getLast().lastEvent().reason()).isEqualTo(LanternReason.LOW_LUX);
    }

    @Test
    /**
     * Erwartet, dass der ESP32 nach ausbleibenden Meldungen automatisch als offline markiert wird.
     */
    void marksEsp32OfflineAfterHeartbeatTimeout() {
        LanternRealtimeService realtimeService = mock(LanternRealtimeService.class);
        LanternStateService lanternStateService = new LanternStateService(realtimeService);
        Instant firstStateAt = Instant.parse("2026-04-29T10:15:00Z");

        lanternStateService.handleState(new LanternStatePayload(
                LanternMode.AUTO,
                LightState.ON,
                14.5,
                true,
                50.0
        ), firstStateAt);

        lanternStateService.expireStaleDeviceIfNecessary(firstStateAt.plus(LanternStateService.DEVICE_OFFLINE_TIMEOUT).plusSeconds(1));

        assertThat(lanternStateService.getSnapshot().state().online()).isFalse();
        verify(realtimeService, times(2)).broadcast(org.mockito.ArgumentMatchers.any());
    }
}
