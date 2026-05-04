package fes.smartown.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.lanterns.model.LanternCommandPayload;
import fes.smartown.backend.lanterns.model.LanternEventPayload;
import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternReason;
import fes.smartown.backend.lanterns.model.LanternStatePayload;
import fes.smartown.backend.lanterns.model.LightState;
import fes.smartown.backend.lanterns.service.LanternStateService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueft den Laternenadapter ueber der allgemeinen MQTT-Infrastruktur.
 */
class MqttLanternBridgeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    /**
     * Erwartet, dass State-Nachrichten an den LanternStateService weitergereicht werden.
     */
    void routesStateMessagesToLanternStateService() throws Exception {
        LanternStateService lanternStateService = mock(LanternStateService.class);
        MqttLanternBridge bridge = bridge(lanternStateService, mock(MqttClient.class), mock(ScheduledExecutorService.class));
        LanternStatePayload payload = new LanternStatePayload(LanternMode.AUTO, LightState.ON, 14.5, true, 50.0);

        bridge.messageArrived("smartown/lanterns/state", message(payload));

        ArgumentCaptor<LanternStatePayload> captor = ArgumentCaptor.forClass(LanternStatePayload.class);
        verify(lanternStateService).handleState(captor.capture());
        assertThat(captor.getValue()).isEqualTo(payload);
    }

    @Test
    /**
     * Erwartet, dass Event-Nachrichten an den LanternStateService weitergereicht werden.
     */
    void routesEventMessagesToLanternStateService() throws Exception {
        LanternStateService lanternStateService = mock(LanternStateService.class);
        MqttLanternBridge bridge = bridge(lanternStateService, mock(MqttClient.class), mock(ScheduledExecutorService.class));
        LanternEventPayload payload = new LanternEventPayload("LIGHT_STATE_CHANGED", LightState.ON, LanternReason.LOW_LUX);

        bridge.messageArrived("smartown/lanterns/event", message(payload));

        ArgumentCaptor<LanternEventPayload> captor = ArgumentCaptor.forClass(LanternEventPayload.class);
        verify(lanternStateService).handleEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(payload);
    }

    @Test
    /**
     * Erwartet, dass ein Moduswechsel auf das Laternen-Command-Topic publiziert wird.
     */
    void publishesLanternModeCommandsToCommandTopic() throws Exception {
        LanternStateService lanternStateService = mock(LanternStateService.class);
        MqttClient mqttClient = mock(MqttClient.class);
        ScheduledExecutorService reconnectExecutor = mock(ScheduledExecutorService.class);
        when(mqttClient.isConnected()).thenReturn(true);
        MqttLanternBridge bridge = bridge(lanternStateService, mqttClient, reconnectExecutor);

        bridge.publishModeCommand(LanternMode.FORCED_OFF);

        verify(mqttClient).connect(any());
        verify(mqttClient).subscribe("smartown/lanterns/state", 1);
        verify(mqttClient).subscribe("smartown/lanterns/event", 1);
        verify(lanternStateService).updateBrokerConnection(true);

        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("smartown/lanterns/command"), messageCaptor.capture());

        LanternCommandPayload payload = objectMapper.readValue(
                new String(messageCaptor.getValue().getPayload(), StandardCharsets.UTF_8),
                LanternCommandPayload.class
        );
        assertThat(payload.action()).isEqualTo("SET_MODE");
        assertThat(payload.mode()).isEqualTo(LanternMode.FORCED_OFF);
    }

    @Test
    /**
     * Erwartet, dass unbekannte Topics bewusst ignoriert werden.
     */
    void ignoresUnknownTopics() throws Exception {
        LanternStateService lanternStateService = mock(LanternStateService.class);
        MqttLanternBridge bridge = bridge(lanternStateService, mock(MqttClient.class), mock(ScheduledExecutorService.class));

        bridge.messageArrived("smartown/bridge/state", message(Map.of("status", "OPEN")));

        verifyNoInteractions(lanternStateService);
    }

    private MqttLanternBridge bridge(LanternStateService lanternStateService,
                                     MqttClient mqttClient,
                                     ScheduledExecutorService reconnectExecutor) {
        return new MqttLanternBridge(
                properties(),
                lanternStateService,
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
