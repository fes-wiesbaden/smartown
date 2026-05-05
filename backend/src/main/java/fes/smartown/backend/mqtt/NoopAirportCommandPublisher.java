package fes.smartown.backend.mqtt;

import fes.smartown.backend.airport.model.AirportMode;
import fes.smartown.backend.airport.service.AirportCommandPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "smartown.mqtt.enabled", havingValue = "false")
public class NoopAirportCommandPublisher implements AirportCommandPublisher {

    @Override
    public void publishModeCommand(AirportMode mode) {
        throw new IllegalStateException("MQTT integration disabled");
    }
}
