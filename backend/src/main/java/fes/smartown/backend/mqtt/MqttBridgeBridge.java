package fes.smartown.backend.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.bridge.service.BridgeCommandPublisher;
import fes.smartown.backend.bridge.service.BridgeStateService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "smartown.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttBridgeBridge extends AbstractMqttBridge implements BridgeCommandPublisher {

    private static final String EVENT_TOPIC = "smartown/bridge/event";
    private static final String COMMAND_TOPIC = "smartown/bridge/command";

    private final BridgeStateService bridgeStateService;

    @Autowired
    public MqttBridgeBridge(MqttProperties properties,
                            ObjectMapper objectMapper,
                            @Lazy BridgeStateService bridgeStateService) {
        super(properties, objectMapper, "smartown-backend-bridge-");
        this.bridgeStateService = bridgeStateService;
    }

    @Override
    public void publishCommand(String command) {
        // Das Backend sendet "OPEN" oder "CLOSE"
        publish(COMMAND_TOPIC, command);
    }

    @Override
    protected void subscribeTopics(MqttClient mqttClient) throws MqttException {
        mqttClient.subscribe(EVENT_TOPIC, 1);
    }

    @Override
    protected void handleMessage(String topic, String payload) {
        if (EVENT_TOPIC.equals(topic)) {
            // Falls Jackson Strings mit Anführungszeichen versieht ("..."), diese entfernen
            String cleanPayload = payload.replace("\"", "").trim();
            bridgeStateService.handleEvent(cleanPayload);
        }
    }

    @Override
    protected void onBrokerConnectionChanged(boolean brokerConnected) {
        // Hier könnte der Status später für das Frontend gespeichert werden
    }
}
