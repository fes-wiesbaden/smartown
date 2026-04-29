package fes.smartown.backend.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.lanterns.model.LanternCommandPayload;
import fes.smartown.backend.lanterns.model.LanternEventPayload;
import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternStatePayload;
import fes.smartown.backend.lanterns.service.LanternCommandPublisher;
import fes.smartown.backend.lanterns.service.LanternStateService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

@Service
@ConditionalOnProperty(name = "smartown.mqtt.enabled", havingValue = "true", matchIfMissing = true)
/**
 * Verbindet Backend und MQTT-Broker fuer den Laternen-MVP in beide Richtungen.
 * Weitere Module kommen nicht in diese Klasse, sondern als eigene Adapter wie
 * z. B. MqttAirportBridge mit Airport-Payloads oder MqttBridgeBridge mit
 * Bruecken-Payloads auf Basis von AbstractMqttBridge.
 */
public class MqttLanternBridge extends AbstractMqttBridge implements LanternCommandPublisher {

    private static final String STATE_TOPIC = "smartown/lanterns/state";
    private static final String EVENT_TOPIC = "smartown/lanterns/event";
    private static final String COMMAND_TOPIC = "smartown/lanterns/command";
    private static final String COMMAND_ACTION = "SET_MODE";

    private final LanternStateService lanternStateService;

    @Autowired
    public MqttLanternBridge(MqttProperties properties,
                             LanternStateService lanternStateService,
                             ObjectMapper objectMapper) {
        super(properties, objectMapper, "smartown-backend-");
        this.lanternStateService = lanternStateService;
    }

    MqttLanternBridge(MqttProperties properties,
                      LanternStateService lanternStateService,
                      ObjectMapper objectMapper,
                      ScheduledExecutorService reconnectExecutor,
                      MqttClientFactory mqttClientFactory) {
        super(properties, objectMapper, reconnectExecutor, mqttClientFactory, "smartown-backend-");
        this.lanternStateService = lanternStateService;
    }

    @Override
    /**
     * Wandelt einen REST-Moduswechsel in ein MQTT-Command-Payload um.
     */
    public void publishModeCommand(LanternMode mode) {
        Objects.requireNonNull(mode, "mode");
        publish(COMMAND_TOPIC, new LanternCommandPayload(COMMAND_ACTION, mode));
    }

    @Override
    /**
     * Registriert die fuer die Laternen benoetigten Topics nach erfolgreicher Verbindung.
     */
    protected void subscribeTopics(MqttClient mqttClient) throws MqttException {
        mqttClient.subscribe(STATE_TOPIC, 1);
        mqttClient.subscribe(EVENT_TOPIC, 1);
    }

    @Override
    /**
     * Ordnet eingehende MQTT-Nachrichten dem passenden State- oder Event-Handler zu.
     */
    protected void handleMessage(String topic, String payload) throws JsonProcessingException {
        if (STATE_TOPIC.equals(topic)) {
            lanternStateService.handleState(readPayload(payload, LanternStatePayload.class));
            return;
        }

        if (EVENT_TOPIC.equals(topic)) {
            lanternStateService.handleEvent(readPayload(payload, LanternEventPayload.class));
        }
    }

    @Override
    /**
     * Spiegelt den Broker-Zustand in den Laternen-Snapshot.
     */
    protected void onBrokerConnectionChanged(boolean brokerConnected) {
        lanternStateService.updateBrokerConnection(brokerConnected);
    }
}
