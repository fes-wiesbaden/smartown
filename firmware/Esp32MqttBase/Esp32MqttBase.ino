#include <WiFi.h>
#include <PubSubClient.h>

#include "secrets.h"

namespace {
constexpr uint8_t LED_PIN = 32;
constexpr unsigned long PUBLISH_INTERVAL_MS = 5000;

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
unsigned long lastPublishMs = 0;

void ensureWifiConnected() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
}

void handleCommand(char *topic, byte *payload, unsigned int length) {
  String message;
  message.reserve(length);
  for (unsigned int i = 0; i < length; ++i) {
    message += static_cast<char>(payload[i]);
  }

  if (String(topic) != MQTT_TOPIC_COMMAND) {
    return;
  }

  if (message == "on") {
    digitalWrite(LED_PIN, HIGH);
  } else if (message == "off") {
    digitalWrite(LED_PIN, LOW);
  } else if (message == "toggle") {
    digitalWrite(LED_PIN, !digitalRead(LED_PIN));
  }
}

void ensureMqttConnected() {
  while (!mqttClient.connected()) {
    if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
      mqttClient.subscribe(MQTT_TOPIC_COMMAND);
      mqttClient.publish(MQTT_TOPIC_STATE, digitalRead(LED_PIN) ? "on" : "off", true);
      return;
    }

    delay(2000);
  }
}

void publishHeartbeat() {
  const unsigned long now = millis();
  if (now - lastPublishMs < PUBLISH_INTERVAL_MS) {
    return;
  }

  lastPublishMs = now;
  mqttClient.publish(MQTT_TOPIC_STATE, digitalRead(LED_PIN) ? "on" : "off", true);
}
}  // namespace

void setup() {
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);

  Serial.begin(115200);
  ensureWifiConnected();

  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);
  ensureMqttConnected();
}

void loop() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();
  publishHeartbeat();
}
