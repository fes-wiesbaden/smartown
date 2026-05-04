/**
 * KI-Hinweis:
 * Diese Testklasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.mqtt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MqttPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class
            ))
            .withUserConfiguration(MqttPropertiesTestConfiguration.class);

    @Test
    void bindsComposeStyleEnvironmentVariables() {
        contextRunner
                .withPropertyValues(
                        "mqtt.broker-url=tcp://${SPRING_MQTT_HOST:${MQTT_HOST:localhost}}:${SPRING_MQTT_PORT:${MQTT_PORT:1883}}",
                        "mqtt.username=${SPRING_MQTT_USERNAME:${MQTT_USERNAME:}}",
                        "mqtt.password=${SPRING_MQTT_PASSWORD:${MQTT_PASSWORD:}}",
                        "MQTT_HOST=broker",
                        "MQTT_PORT=2883",
                        "MQTT_USERNAME=smartown",
                        "MQTT_PASSWORD=secret"
                )
                .run(context -> {
                    MqttProperties properties = context.getBean(MqttProperties.class);

                    assertThat(properties.getBrokerUrl()).isEqualTo("tcp://broker:2883");
                    assertThat(properties.getUsername()).isEqualTo("smartown");
                    assertThat(properties.getPassword()).isEqualTo("secret");
                });
    }

    @Test
    void prefersSpringSpecificOverrides() {
        contextRunner
                .withPropertyValues(
                        "mqtt.broker-url=tcp://${SPRING_MQTT_HOST:${MQTT_HOST:localhost}}:${SPRING_MQTT_PORT:${MQTT_PORT:1883}}",
                        "mqtt.username=${SPRING_MQTT_USERNAME:${MQTT_USERNAME:}}",
                        "mqtt.password=${SPRING_MQTT_PASSWORD:${MQTT_PASSWORD:}}",
                        "MQTT_HOST=broker",
                        "MQTT_PORT=2883",
                        "MQTT_USERNAME=smartown",
                        "MQTT_PASSWORD=secret",
                        "SPRING_MQTT_HOST=override-broker",
                        "SPRING_MQTT_PORT=3883",
                        "SPRING_MQTT_USERNAME=override-user",
                        "SPRING_MQTT_PASSWORD=override-password"
                )
                .run(context -> {
                    MqttProperties properties = context.getBean(MqttProperties.class);

                    assertThat(properties.getBrokerUrl()).isEqualTo("tcp://override-broker:3883");
                    assertThat(properties.getUsername()).isEqualTo("override-user");
                    assertThat(properties.getPassword()).isEqualTo("override-password");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MqttProperties.class)
    static class MqttPropertiesTestConfiguration {
    }
}
