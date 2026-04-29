package fes.smartown.backend.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stellt die gemeinsame MQTT-Infrastruktur fuer modulbezogene Adapter bereit.
 * Weitere Module wie Airport oder Bridge bekommen eigene Adapterklassen auf
 * Basis dieser Klasse und mappen dort ihre eigenen Topics und Payload-Typen.
 */
abstract class AbstractMqttBridge implements MqttCallback {

    private static final long RECONNECT_DELAY_SECONDS = 5;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MqttProperties properties;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService reconnectExecutor;
    private final MqttClientFactory mqttClientFactory;
    private final String clientIdPrefix;
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    private MqttClient mqttClient;

    protected AbstractMqttBridge(MqttProperties properties,
                                 ObjectMapper objectMapper,
                                 String clientIdPrefix) {
        this(
                properties,
                objectMapper,
                Executors.newSingleThreadScheduledExecutor(),
                AbstractMqttBridge::createDefaultClient,
                clientIdPrefix
        );
    }

    AbstractMqttBridge(MqttProperties properties,
                       ObjectMapper objectMapper,
                       ScheduledExecutorService reconnectExecutor,
                       MqttClientFactory mqttClientFactory,
                       String clientIdPrefix) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.reconnectExecutor = Objects.requireNonNull(reconnectExecutor, "reconnectExecutor");
        this.mqttClientFactory = Objects.requireNonNull(mqttClientFactory, "mqttClientFactory");
        this.clientIdPrefix = Objects.requireNonNull(clientIdPrefix, "clientIdPrefix");
    }

    @PostConstruct
    /**
     * Baut direkt nach dem Start die erste Broker-Verbindung auf.
     */
    final void connectOnStartup() {
        connectIfNecessary();
    }

    @PreDestroy
    /**
     * Beendet Reconnect-Tasks und trennt die MQTT-Verbindung beim Shutdown.
     */
    final void shutdown() {
        reconnectExecutor.shutdownNow();
        disconnectQuietly();
    }

    /**
     * Serialisiert und versendet ein fachliches Payload an das angegebene Topic.
     */
    protected final void publish(String topic, Object payload) {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(payload, "payload");
        connectIfNecessary();
        if (!isConnected()) {
            throw new IllegalStateException("MQTT broker unavailable");
        }

        try {
            mqttClient.publish(topic, toMessage(payload));
        } catch (MqttException | JsonProcessingException exception) {
            throw new IllegalStateException("MQTT command could not be sent", exception);
        }
    }

    /**
     * Deserialisiert ein JSON-Payload in den angeforderten Nachrichtentyp.
     */
    protected final <T> T readPayload(String payload, Class<T> payloadType) throws JsonProcessingException {
        return objectMapper.readValue(payload, payloadType);
    }

    @Override
    /**
     * Markiert den Broker als getrennt und plant einen kontrollierten Reconnect.
     */
    public final void connectionLost(Throwable cause) {
        onBrokerConnectionChanged(false);
        scheduleReconnect();
        logger.warn("MQTT connection lost", cause);
    }

    @Override
    /**
     * Leitet eingehende MQTT-Nachrichten an den fachlichen Adapter weiter.
     */
    public final void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        try {
            handleMessage(topic, payload);
        } catch (JsonProcessingException exception) {
            logger.warn("Could not parse MQTT payload from topic {}: {}", topic, payload, exception);
        }
    }

    @Override
    /**
     * Wird von Paho nach erfolgreicher Zustellung aufgerufen. Mehr Lifecycle braucht das REST-API hier nicht.
     */
    public final void deliveryComplete(IMqttDeliveryToken token) {
    }

    /**
     * Baut die Broker-Verbindung nur dann auf, wenn aktuell keine nutzbare Verbindung existiert.
     */
    private synchronized void connectIfNecessary() {
        if (isConnected()) {
            return;
        }

        try {
            if (mqttClient == null) {
                mqttClient = mqttClientFactory.create(properties.getBrokerUrl(), clientIdPrefix + UUID.randomUUID());
                mqttClient.setCallback(this);
            }

            mqttClient.connect(connectOptions());
            subscribeTopics(mqttClient);
            onBrokerConnectionChanged(true);
            reconnectScheduled.set(false);
            logger.info("Connected to MQTT broker at {}", properties.getBrokerUrl());
        } catch (MqttException exception) {
            onBrokerConnectionChanged(false);
            scheduleReconnect();
            logger.warn("MQTT connect failed", exception);
        }
    }

    /**
     * Prueft kompakt, ob der interne MQTT-Client aktuell verbunden ist.
     */
    private boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * Plant genau einen Reconnect-Versuch mit kurzem Delay, um Broker-Ausfaelle abzufedern.
     */
    private void scheduleReconnect() {
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            connectIfNecessary();
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Baut die Paho-Connect-Optionen aus den konfigurierten Zugangsdaten.
     */
    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);

        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            options.setUserName(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            options.setPassword(properties.getPassword().toCharArray());
        }

        return options;
    }

    /**
     * Serialisiert ein Java-Payload in die MQTT-Nachrichtenform des Brokers.
     */
    private MqttMessage toMessage(Object payload) throws JsonProcessingException {
        MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(payload));
        message.setQos(1);
        message.setRetained(false);
        return message;
    }

    /**
     * Trennt die Verbindung beim Shutdown ohne weitere Fehler nach oben zu reichen.
     */
    private synchronized void disconnectQuietly() {
        if (mqttClient == null) {
            return;
        }

        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            mqttClient.close();
        } catch (MqttException exception) {
            logger.debug("Ignoring MQTT disconnect failure", exception);
        }
    }

    private static MqttClient createDefaultClient(String brokerUrl, String clientId) throws MqttException {
        return new MqttClient(brokerUrl, clientId, new MemoryPersistence());
    }

    /**
     * Registriert alle fachlich benoetigten Subscriptions nach erfolgreicher Verbindung.
     * Beispiel spaeter:
     * - Airport-Adapter subscribed auf smartown/airport/state und .../event
     * - Bridge-Adapter subscribed auf smartown/bridge/state und .../event
     */
    protected abstract void subscribeTopics(MqttClient mqttClient) throws MqttException;

    /**
     * Verarbeitet ein eingehendes MQTT-Payload modulbezogen.
     * Hier deserialisiert jedes Modul seine eigenen Payloads, z. B.
     * AirportStatePayload oder BridgeEventPayload.
     */
    protected abstract void handleMessage(String topic, String payload) throws JsonProcessingException;

    /**
     * Reagiert auf Broker-Statusaenderungen im zugehoerigen Modul.
     * Jedes Modul entscheidet selbst, wie es den Broker-Status in seinen
     * eigenen Snapshot oder Service spiegelt.
     */
    protected abstract void onBrokerConnectionChanged(boolean brokerConnected);

    @FunctionalInterface
    interface MqttClientFactory {
        MqttClient create(String brokerUrl, String clientId) throws MqttException;
    }
}
