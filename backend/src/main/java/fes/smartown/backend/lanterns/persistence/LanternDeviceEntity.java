package fes.smartown.backend.lanterns.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "lantern_device")
public class LanternDeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_key", nullable = false, unique = true, length = 64)
    private String deviceKey;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "host_ip_address", length = 45)
    private String hostIpAddress;

    @Column(name = "mqtt_broker_host", nullable = false)
    private String mqttBrokerHost;

    @Column(name = "mqtt_broker_port", nullable = false)
    private int mqttBrokerPort;

    @Column(name = "state_topic", nullable = false)
    private String stateTopic;

    @Column(name = "event_topic", nullable = false)
    private String eventTopic;

    @Column(name = "command_topic", nullable = false)
    private String commandTopic;

    @Column(name = "sampling_interval_minutes", nullable = false)
    private int samplingIntervalMinutes;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected LanternDeviceEntity() {
    }

    public LanternDeviceEntity(String deviceKey,
                               String displayName,
                               String hostIpAddress,
                               String mqttBrokerHost,
                               int mqttBrokerPort,
                               String stateTopic,
                               String eventTopic,
                               String commandTopic,
                               int samplingIntervalMinutes,
                               boolean active) {
        this.deviceKey = Objects.requireNonNull(deviceKey, "deviceKey");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.hostIpAddress = hostIpAddress;
        this.mqttBrokerHost = Objects.requireNonNull(mqttBrokerHost, "mqttBrokerHost");
        if (mqttBrokerPort <= 0) {
            throw new IllegalArgumentException("mqttBrokerPort must be greater than zero");
        }
        if (samplingIntervalMinutes <= 0) {
            throw new IllegalArgumentException("samplingIntervalMinutes must be greater than zero");
        }
        this.mqttBrokerPort = mqttBrokerPort;
        this.stateTopic = Objects.requireNonNull(stateTopic, "stateTopic");
        this.eventTopic = Objects.requireNonNull(eventTopic, "eventTopic");
        this.commandTopic = Objects.requireNonNull(commandTopic, "commandTopic");
        this.samplingIntervalMinutes = samplingIntervalMinutes;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHostIpAddress() {
        return hostIpAddress;
    }

    public String getMqttBrokerHost() {
        return mqttBrokerHost;
    }

    public int getMqttBrokerPort() {
        return mqttBrokerPort;
    }

    public String getStateTopic() {
        return stateTopic;
    }

    public String getEventTopic() {
        return eventTopic;
    }

    public String getCommandTopic() {
        return commandTopic;
    }

    public int getSamplingIntervalMinutes() {
        return samplingIntervalMinutes;
    }

    public boolean isActive() {
        return active;
    }
}
