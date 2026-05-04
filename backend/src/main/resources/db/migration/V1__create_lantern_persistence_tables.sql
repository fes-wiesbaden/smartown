-- KI-Hinweis:
-- Diese Migration wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
-- Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
-- Die Migration wurde projektbezogen geprüft und validiert.
CREATE TABLE lantern_device (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    controller_ip_address VARCHAR(45) NULL,
    mqtt_broker_host VARCHAR(255) NOT NULL,
    mqtt_broker_port INT NOT NULL,
    state_topic VARCHAR(255) NOT NULL,
    event_topic VARCHAR(255) NOT NULL,
    command_topic VARCHAR(255) NOT NULL,
    sampling_interval_minutes INT NOT NULL,
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_lantern_device_device_key UNIQUE (device_key),
    CONSTRAINT chk_lantern_device_sampling_interval_positive CHECK (sampling_interval_minutes > 0),
    CONSTRAINT chk_lantern_device_mqtt_port_positive CHECK (mqtt_broker_port > 0)
);

CREATE TABLE lantern_lux_measurement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lantern_device_id BIGINT NOT NULL,
    lux DECIMAL(10, 2) NOT NULL,
    measured_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lantern_lux_measurement_device
        FOREIGN KEY (lantern_device_id) REFERENCES lantern_device (id),
    INDEX idx_lantern_lux_measurement_device_measured_at (lantern_device_id, measured_at)
);

INSERT INTO lantern_device (
    device_key,
    display_name,
    controller_ip_address,
    mqtt_broker_host,
    mqtt_broker_port,
    state_topic,
    event_topic,
    command_topic,
    sampling_interval_minutes,
    active
) VALUES (
    'LANTERN_SENSOR_CONTROLLER',
    'Laternencontroller mit BH1750',
    NULL,
    'mqtt',
    1883,
    'smartown/lanterns/state',
    'smartown/lanterns/event',
    'smartown/lanterns/command',
    5,
    TRUE
);
