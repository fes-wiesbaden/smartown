package fes.smartown.backend.lanterns.service;

import fes.smartown.backend.lanterns.model.LanternLuxHistoryPoint;
import fes.smartown.backend.lanterns.persistence.LanternDeviceRepository;
import fes.smartown.backend.lanterns.persistence.LanternLuxMeasurementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LanternLuxHistoryService {

    private final LanternDeviceRepository lanternDeviceRepository;
    private final LanternLuxMeasurementRepository lanternLuxMeasurementRepository;

    public LanternLuxHistoryService(LanternDeviceRepository lanternDeviceRepository,
                                    LanternLuxMeasurementRepository lanternLuxMeasurementRepository) {
        this.lanternDeviceRepository = lanternDeviceRepository;
        this.lanternLuxMeasurementRepository = lanternLuxMeasurementRepository;
    }

    /**
     * Liefert die vollstaendige Lux-Historie der aktiven Laternenkonfiguration als eine Zeitreihe.
     */
    @Transactional(readOnly = true)
    public List<LanternLuxHistoryPoint> getHistory() {
        return lanternDeviceRepository.findTopByActiveTrueOrderByIdAsc()
                .map(device -> lanternLuxMeasurementRepository.findByLanternDeviceIdOrderByMeasuredAtAsc(device.getId()))
                .orElseGet(List::of)
                .stream()
                .map(measurement -> new LanternLuxHistoryPoint(
                        measurement.getMeasuredAt(),
                        measurement.getLux().doubleValue()
                ))
                .toList();
    }
}
