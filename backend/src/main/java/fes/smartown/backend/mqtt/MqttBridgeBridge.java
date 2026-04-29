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
    private static final String STATE_TOPIC = "smartown/bridge/state";

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
        publish(COMMAND_TOPIC, command);
    }

    @Override
    protected void subscribeTopics(MqttClient mqttClient) throws MqttException {
        mqttClient.subscribe(EVENT_TOPIC, 1);
        mqttClient.subscribe(STATE_TOPIC, 1);
    }

    @Override
    protected void handleMessage(String topic, String payload) {
        if (EVENT_TOPIC.equals(topic)) {
            String cleanPayload = payload.replace("\"", "").trim();
            bridgeStateService.handleEvent(cleanPayload);
        } else if (STATE_TOPIC.equals(topic)) {
            bridgeStateService.handleHeartbeat();
        }
    }

    @Override
    protected void onBrokerConnectionChanged(boolean brokerConnected) {
        bridgeStateService.updateBrokerConnection(brokerConnected);
    }
}
