package fes.smartown.backend.lanterns.persistence;

import fes.smartown.backend.lanterns.model.LanternSnapshot;
import fes.smartown.backend.lanterns.service.LanternStateService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class LanternLuxSamplingService {

    private final LanternStateService lanternStateService;
    private final LanternDeviceRepository lanternDeviceRepository;
    private final LanternLuxMeasurementRepository lanternLuxMeasurementRepository;

    public LanternLuxSamplingService(LanternStateService lanternStateService,
                                     LanternDeviceRepository lanternDeviceRepository,
                                     LanternLuxMeasurementRepository lanternLuxMeasurementRepository) {
        this.lanternStateService = lanternStateService;
        this.lanternDeviceRepository = lanternDeviceRepository;
        this.lanternLuxMeasurementRepository = lanternLuxMeasurementRepository;
    }

    /**
     * Persistiert Lux-Messwerte in festem Takt, solange die Laterne aktiv ist und gueltige Telemetrie liefert.
     */
    @Scheduled(fixedDelayString = "${smartown.lanterns.lux-persistence-poll-ms:60000}")
    void persistCurrentLuxIfDue() {
        persistCurrentLuxIfDue(Instant.now());
    }

    @Transactional
    void persistCurrentLuxIfDue(Instant now) {
        Objects.requireNonNull(now, "now");

        LanternSnapshot snapshot = lanternStateService.getSnapshot();
        Double lux = snapshot.state().lux();
        if (!snapshot.state().online() || lux == null) {
            return;
        }

        List<LanternDeviceEntity> activeDevices = lanternDeviceRepository.findByActiveTrue();
        for (LanternDeviceEntity activeDevice : activeDevices) {
            persistMeasurementIfDue(activeDevice, lux, now);
        }
    }

    private void persistMeasurementIfDue(LanternDeviceEntity lanternDevice, double lux, Instant now) {
        Duration samplingInterval = Duration.ofMinutes(lanternDevice.getSamplingIntervalMinutes());
        Instant nextAllowedAt = lanternLuxMeasurementRepository
                .findTopByLanternDeviceIdOrderByMeasuredAtDesc(lanternDevice.getId())
                .map(LanternLuxMeasurementEntity::getMeasuredAt)
                .map(lastMeasurementAt -> lastMeasurementAt.plus(samplingInterval))
                .orElse(Instant.MIN);

        if (nextAllowedAt.isAfter(now)) {
            return;
        }

        lanternLuxMeasurementRepository.save(
                new LanternLuxMeasurementEntity(
                        lanternDevice,
                        BigDecimal.valueOf(lux),
                        now
                )
        );
    }
}
