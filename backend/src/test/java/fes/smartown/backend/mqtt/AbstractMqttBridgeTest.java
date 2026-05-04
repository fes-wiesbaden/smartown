/**
 * KI-Hinweis:
 * Diese Testklasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueft den gemeinsamen MQTT-Lifecycle unabhaengig von einer Fachdomaene.
 */
class AbstractMqttBridgeTest {

    @Test
    /**
     * Erwartet, dass Start sofort verbindet, subscribt und den Broker als online markiert.
     */
    void connectsOnStartupAndSubscribesTopics() throws Exception {
        MqttClient mqttClient = mock(MqttClient.class);
        ScheduledExecutorService reconnectExecutor = mock(ScheduledExecutorService.class);
        TestMqttBridge bridge = new TestMqttBridge(mqttClient, reconnectExecutor);

        bridge.connectOnStartup();

        verify(mqttClient).setCallback(bridge);
        verify(mqttClient).connect(any());
        verify(mqttClient).subscribe("smartown/test/state", 1);
        verify(reconnectExecutor, times(0)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        assertThat(bridge.brokerConnected()).isTrue();
    }

    @Test
    /**
     * Erwartet genau einen Reconnect-Task, auch wenn mehrfach derselbe Ausfall gemeldet wird.
     */
    void schedulesOnlyOneReconnectPerConnectionLossBurst() {
        MqttClient mqttClient = mock(MqttClient.class);
        ScheduledExecutorService reconnectExecutor = mock(ScheduledExecutorService.class);
        TestMqttBridge bridge = new TestMqttBridge(mqttClient, reconnectExecutor);

        bridge.connectionLost(new RuntimeException("broker down"));
        bridge.connectionLost(new RuntimeException("still down"));

        verify(reconnectExecutor, times(1)).schedule(any(Runnable.class), eq(5L), eq(TimeUnit.SECONDS));
    }

    @Test
    /**
     * Erwartet einen klaren Fehler, wenn ein Publish bei nicht erreichbarem Broker angefordert wird.
     */
    void throwsWhenPublishingWithoutBrokerConnection() throws Exception {
        MqttClient mqttClient = mock(MqttClient.class);
        ScheduledExecutorService reconnectExecutor = mock(ScheduledExecutorService.class);
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mqttClient)
                .connect(any());
        TestMqttBridge bridge = new TestMqttBridge(mqttClient, reconnectExecutor);

        assertThatThrownBy(bridge::publishHeartbeat)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MQTT broker unavailable");
    }

    @Test
    /**
     * Erwartet, dass Shutdown den MQTT-Client sauber trennt und den Executor beendet.
     */
    void disconnectsAndStopsExecutorOnShutdown() throws Exception {
        MqttClient mqttClient = mock(MqttClient.class);
        ScheduledExecutorService reconnectExecutor = mock(ScheduledExecutorService.class);
        when(mqttClient.isConnected()).thenReturn(true);
        TestMqttBridge bridge = new TestMqttBridge(mqttClient, reconnectExecutor);
        bridge.connectOnStartup();

        bridge.shutdown();

        verify(reconnectExecutor).shutdownNow();
        verify(mqttClient).disconnect();
        verify(mqttClient).close();
    }

    private static final class TestMqttBridge extends AbstractMqttBridge {

        private boolean brokerConnected;

        private TestMqttBridge(MqttClient mqttClient, ScheduledExecutorService reconnectExecutor) {
            super(properties(), new ObjectMapper(), reconnectExecutor, (brokerUrl, clientId) -> mqttClient, "smartown-test-");
        }

        private void publishHeartbeat() {
            publish("smartown/test/command", java.util.Map.of("action", "PING"));
        }

        private boolean brokerConnected() {
            return brokerConnected;
        }

        @Override
        protected void subscribeTopics(MqttClient mqttClient) throws MqttException {
            mqttClient.subscribe("smartown/test/state", 1);
        }

        @Override
        protected void handleMessage(String topic, String payload) {
        }

        @Override
        protected void onBrokerConnectionChanged(boolean brokerConnected) {
            this.brokerConnected = brokerConnected;
        }

        private static MqttProperties properties() {
            MqttProperties properties = new MqttProperties();
            properties.setBrokerUrl("tcp://localhost:1883");
            return properties;
        }
    }
}
