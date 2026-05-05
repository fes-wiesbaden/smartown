package fes.smartown.backend.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.airport.model.AirportCommandPayload;
import fes.smartown.backend.airport.model.AirportMode;
import fes.smartown.backend.airport.model.AirportStatePayload;
import fes.smartown.backend.airport.service.AirportCommandPublisher;
import fes.smartown.backend.airport.service.AirportStateService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

@Service
@ConditionalOnProperty(name = "smartown.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttAirportBridge extends AbstractMqttBridge implements AirportCommandPublisher {

    private static final String STATE_TOPIC = "smartown/airport/state";
    private static final String COMMAND_TOPIC = "smartown/airport/command";
    private static final String COMMAND_ACTION = "SET_MODE";

    private final AirportStateService airportStateService;

    @Autowired
    public MqttAirportBridge(MqttProperties properties,
                             AirportStateService airportStateService,
                             ObjectMapper objectMapper) {
        super(properties, objectMapper, "smartown-backend-airport-");
        this.airportStateService = airportStateService;
    }

    MqttAirportBridge(MqttProperties properties,
                      AirportStateService airportStateService,
                      ObjectMapper objectMapper,
                      ScheduledExecutorService reconnectExecutor,
                      MqttClientFactory mqttClientFactory) {
        super(properties, objectMapper, reconnectExecutor, mqttClientFactory, "smartown-backend-airport-");
        this.airportStateService = airportStateService;
    }

    @Override
    public void publishModeCommand(AirportMode mode) {
        Objects.requireNonNull(mode, "mode");
        publish(COMMAND_TOPIC, new AirportCommandPayload(COMMAND_ACTION, mode));
    }

    @Override
    protected void subscribeTopics(MqttClient mqttClient) throws MqttException {
        mqttClient.subscribe(STATE_TOPIC, 1);
    }

    @Override
    protected void handleMessage(String topic, String payload) throws JsonProcessingException {
        if (STATE_TOPIC.equals(topic)) {
            airportStateService.handleState(readPayload(payload, AirportStatePayload.class));
        }
    }

    @Override
    protected void onBrokerConnectionChanged(boolean brokerConnected) {
        airportStateService.updateBrokerConnection(brokerConnected);
    }
}
