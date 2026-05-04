/**
 * KI-Hinweis:
 * Diese Testklasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.persistence;

import fes.smartown.backend.lanterns.model.LanternEventPayload;
import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternReason;
import fes.smartown.backend.lanterns.model.LanternSnapshot;
import fes.smartown.backend.lanterns.model.LanternStatePayload;
import fes.smartown.backend.lanterns.model.LightState;
import fes.smartown.backend.lanterns.service.LanternStateService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LanternLuxSamplingServiceTest {

    @Test
    void persistsLuxMeasurementForActiveLanternWhenNoRecentEntryExists() {
        LanternStateService lanternStateService = mock(LanternStateService.class);
        LanternDeviceRepository lanternDeviceRepository = mock(LanternDeviceRepository.class);
        LanternLuxMeasurementRepository lanternLuxMeasurementRepository = mock(LanternLuxMeasurementRepository.class);
        LanternLuxSamplingService samplingService = new LanternLuxSamplingService(
                lanternStateService,
                lanternDeviceRepository,
                lanternLuxMeasurementRepository
        );

        LanternDeviceEntity activeDevice = activeDevice(1L, 5);
        Instant measuredAt = Instant.parse("2026-05-05T08:15:00Z");
        when(lanternStateService.getSnapshot()).thenReturn(snapshot(true, 18.75, measuredAt));
        when(lanternDeviceRepository.findByActiveTrue()).thenReturn(List.of(activeDevice));
        when(lanternLuxMeasurementRepository.findTopByLanternDeviceIdOrderByMeasuredAtDesc(1L))
                .thenReturn(Optional.empty());

        samplingService.persistCurrentLuxIfDue(measuredAt);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(LanternLuxMeasurementEntity.class);
        verify(lanternLuxMeasurementRepository).save(captor.capture());
        assertThat(captor.getValue().getLanternDevice()).isEqualTo(activeDevice);
        assertThat(captor.getValue().getLux()).isEqualByComparingTo(BigDecimal.valueOf(18.75));
        assertThat(captor.getValue().getMeasuredAt()).isEqualTo(measuredAt);
    }

    @Test
    void skipsPersistenceWhenLanternIsOffline() {
        LanternStateService lanternStateService = mock(LanternStateService.class);
        LanternDeviceRepository lanternDeviceRepository = mock(LanternDeviceRepository.class);
        LanternLuxMeasurementRepository lanternLuxMeasurementRepository = mock(LanternLuxMeasurementRepository.class);
        LanternLuxSamplingService samplingService = new LanternLuxSamplingService(
                lanternStateService,
                lanternDeviceRepository,
                lanternLuxMeasurementRepository
        );

        when(lanternStateService.getSnapshot()).thenReturn(snapshot(false, 17.2, Instant.parse("2026-05-05T08:15:00Z")));

        samplingService.persistCurrentLuxIfDue(Instant.parse("2026-05-05T08:15:00Z"));

        verifyNoInteractions(lanternDeviceRepository, lanternLuxMeasurementRepository);
    }

    @Test
    void skipsPersistenceWhenLuxIsMissing() {
        LanternStateService lanternStateService = mock(LanternStateService.class);
        LanternDeviceRepository lanternDeviceRepository = mock(LanternDeviceRepository.class);
        LanternLuxMeasurementRepository lanternLuxMeasurementRepository = mock(LanternLuxMeasurementRepository.class);
        LanternLuxSamplingService samplingService = new LanternLuxSamplingService(
                lanternStateService,
                lanternDeviceRepository,
                lanternLuxMeasurementRepository
        );

        when(lanternStateService.getSnapshot()).thenReturn(snapshot(true, null, Instant.parse("2026-05-05T08:15:00Z")));

        samplingService.persistCurrentLuxIfDue(Instant.parse("2026-05-05T08:15:00Z"));

        verifyNoInteractions(lanternDeviceRepository, lanternLuxMeasurementRepository);
    }

    @Test
    void skipsPersistenceWhenLatestMeasurementIsStillInsideSamplingInterval() {
        LanternStateService lanternStateService = mock(LanternStateService.class);
        LanternDeviceRepository lanternDeviceRepository = mock(LanternDeviceRepository.class);
        LanternLuxMeasurementRepository lanternLuxMeasurementRepository = mock(LanternLuxMeasurementRepository.class);
        LanternLuxSamplingService samplingService = new LanternLuxSamplingService(
                lanternStateService,
                lanternDeviceRepository,
                lanternLuxMeasurementRepository
        );

        LanternDeviceEntity activeDevice = activeDevice(1L, 5);
        Instant now = Instant.parse("2026-05-05T08:15:00Z");
        when(lanternStateService.getSnapshot()).thenReturn(snapshot(true, 21.0, now));
        when(lanternDeviceRepository.findByActiveTrue()).thenReturn(List.of(activeDevice));
        when(lanternLuxMeasurementRepository.findTopByLanternDeviceIdOrderByMeasuredAtDesc(1L))
                .thenReturn(Optional.of(new LanternLuxMeasurementEntity(
                        activeDevice,
                        BigDecimal.valueOf(19.8),
                        now.minus(Duration.ofMinutes(4))
                )));

        samplingService.persistCurrentLuxIfDue(now);

        verify(lanternLuxMeasurementRepository, never()).save(any());
    }

    private static LanternDeviceEntity activeDevice(Long id, int samplingIntervalMinutes) {
        LanternDeviceEntity lanternDevice = new LanternDeviceEntity(
                "LANTERN_SENSOR_CONTROLLER",
                "Laternencontroller mit BH1750",
                null,
                "mqtt",
                1883,
                "smartown/lanterns/state",
                "smartown/lanterns/event",
                "smartown/lanterns/command",
                samplingIntervalMinutes,
                true
        );
        setId(lanternDevice, id);
        return lanternDevice;
    }

    private static LanternSnapshot snapshot(boolean online, Double lux, Instant updatedAt) {
        return new LanternSnapshot(
                new LanternStatePayload(LanternMode.AUTO, LightState.ON, lux, online, 50.0),
                new LanternEventPayload("LIGHT_STATE_CHANGED", LightState.ON, LanternReason.LOW_LUX),
                true,
                updatedAt
        );
    }

    private static void setId(LanternDeviceEntity lanternDevice, Long id) {
        try {
            var idField = LanternDeviceEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lanternDevice, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not assign lantern device id for test", exception);
        }
    }
}
