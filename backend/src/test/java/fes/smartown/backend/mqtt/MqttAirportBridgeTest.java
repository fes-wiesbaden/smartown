package fes.smartown.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.airport.model.AirportCommandPayload;
import fes.smartown.backend.airport.model.AirportMode;
import fes.smartown.backend.airport.model.AirportStatePayload;
import fes.smartown.backend.airport.service.AirportStateService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MqttAirportBridgeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void routesStateMessagesToAirportStateService() throws Exception {
        AirportStateService airportStateService = mock(AirportStateService.class);
        MqttAirportBridge bridge = bridge(airportStateService, mock(MqttClient.class), mock(ScheduledExecutorService.class));
        AirportStatePayload payload = new AirportStatePayload(AirportMode.ON, true, true);

        bridge.messageArrived("smartown/airport/state", message(payload));

        verify(airportStateService).handleState(payload);
    }

    @Test
    void publishesAirportModeCommandsToCommandTopic() throws Exception {
        AirportStateService airportStateService = mock(AirportStateService.class);
        MqttClient mqttClient = mock(MqttClient.class);
        ScheduledExecutorService reconnectExecutor = mock(ScheduledExecutorService.class);
        when(mqttClient.isConnected()).thenReturn(true);
        MqttAirportBridge bridge = bridge(airportStateService, mqttClient, reconnectExecutor);

        bridge.publishModeCommand(AirportMode.OFF);

        verify(mqttClient).connect(any());
        verify(mqttClient).subscribe("smartown/airport/state", 1);
        verify(airportStateService).updateBrokerConnection(true);

        org.mockito.ArgumentCaptor<MqttMessage> messageCaptor = org.mockito.ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("smartown/airport/command"), messageCaptor.capture());

        AirportCommandPayload payload = objectMapper.readValue(
                new String(messageCaptor.getValue().getPayload(), StandardCharsets.UTF_8),
                AirportCommandPayload.class
        );
        assertThat(payload.action()).isEqualTo("SET_MODE");
        assertThat(payload.mode()).isEqualTo(AirportMode.OFF);
    }

    @Test
    void ignoresUnknownTopics() throws Exception {
        AirportStateService airportStateService = mock(AirportStateService.class);
        MqttAirportBridge bridge = bridge(airportStateService, mock(MqttClient.class), mock(ScheduledExecutorService.class));

        bridge.messageArrived("smartown/bridge/state", message(Map.of("status", "OPEN")));

        verifyNoInteractions(airportStateService);
    }

    @Test
    void marksBrokerConnectedBeforeRetainedStateCanArrive() throws Exception {
        AirportStateService airportStateService = mock(AirportStateService.class);
        MqttClient mqttClient = mock(MqttClient.class);
        ScheduledExecutorService reconnectExecutor = mock(ScheduledExecutorService.class);
        MqttAirportBridge bridge = bridge(airportStateService, mqttClient, reconnectExecutor);
        AirportStatePayload retainedPayload = new AirportStatePayload(AirportMode.ON, true, true);

        doAnswer(invocation -> {
            bridge.messageArrived("smartown/airport/state", message(retainedPayload));
            return null;
        }).when(mqttClient).subscribe("smartown/airport/state", 1);

        bridge.connectOnStartup();

        var inOrder = inOrder(airportStateService);
        inOrder.verify(airportStateService).updateBrokerConnection(true);
        inOrder.verify(airportStateService).handleState(retainedPayload);
    }

    private MqttAirportBridge bridge(AirportStateService airportStateService,
                                     MqttClient mqttClient,
                                     ScheduledExecutorService reconnectExecutor) {
        return new MqttAirportBridge(
                properties(),
                airportStateService,
                objectMapper,
                reconnectExecutor,
                (brokerUrl, clientId) -> mqttClient
        );
    }

    private MqttMessage message(Object payload) throws Exception {
        return new MqttMessage(objectMapper.writeValueAsBytes(payload));
    }

    private static MqttProperties properties() {
        MqttProperties properties = new MqttProperties();
        properties.setBrokerUrl("tcp://localhost:1883");
        return properties;
    }
}
