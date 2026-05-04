package fes.smartown.backend.lanterns.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LanternDeviceRepository extends JpaRepository<LanternDeviceEntity, Long> {

    List<LanternDeviceEntity> findByActiveTrue();
}
