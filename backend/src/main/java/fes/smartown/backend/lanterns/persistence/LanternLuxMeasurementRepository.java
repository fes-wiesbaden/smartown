package fes.smartown.backend.lanterns.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanternLuxMeasurementRepository extends JpaRepository<LanternLuxMeasurementEntity, Long> {

    Optional<LanternLuxMeasurementEntity> findTopByLanternDeviceIdOrderByMeasuredAtDesc(Long lanternDeviceId);
}
