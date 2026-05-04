package fes.smartown.backend.lanterns.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "lantern_lux_measurement")
public class LanternLuxMeasurementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lantern_device_id", nullable = false)
    private LanternDeviceEntity lanternDevice;

    @Column(name = "lux", nullable = false, precision = 10, scale = 2)
    private BigDecimal lux;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    protected LanternLuxMeasurementEntity() {
    }

    public LanternLuxMeasurementEntity(LanternDeviceEntity lanternDevice, BigDecimal lux, Instant measuredAt) {
        this.lanternDevice = Objects.requireNonNull(lanternDevice, "lanternDevice");
        this.lux = Objects.requireNonNull(lux, "lux");
        this.measuredAt = Objects.requireNonNull(measuredAt, "measuredAt");
    }

    public Long getId() {
        return id;
    }

    public LanternDeviceEntity getLanternDevice() {
        return lanternDevice;
    }

    public BigDecimal getLux() {
        return lux;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }
}
