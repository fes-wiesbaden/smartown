#include <ArduinoJson.h>
#include <PubSubClient.h>
#include <WiFi.h>

#include "secrets.h"

namespace {
constexpr uint8_t TRIG_PIN = 5;
constexpr uint8_t ECHO_PIN = 4;
constexpr uint8_t LIGHT_PINS[] = {21, 19, 32, 33, 25, 26, 27, 14, 12, 13};
constexpr size_t LIGHT_COUNT = sizeof(LIGHT_PINS) / sizeof(LIGHT_PINS[0]);
constexpr char TOPIC_COMMAND[] = "smartown/airport/command";
constexpr char TOPIC_STATE[] = "smartown/airport/state";
constexpr char COMMAND_ACTION[] = "SET_MODE";
constexpr unsigned long MQTT_BUFFER_SIZE = 256;
constexpr unsigned long STATE_INTERVAL_MS = 5000;
constexpr unsigned long BLINK_INTERVAL_MS = 100;
constexpr unsigned long TRACK_INTERVAL_MS = 200;
constexpr unsigned long SENSOR_TIMEOUT_US = 30000;
constexpr float MIN_TRACK_DISTANCE_CM = 2.0F;
constexpr float MAX_TRACK_DISTANCE_CM = 40.0F;

enum class AirportMode {
  On,
  Off,
};

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

AirportMode currentMode = AirportMode::Off;
bool lightsOn = false;
int blinkIndex = static_cast<int>(LIGHT_COUNT) - 1;
unsigned long lastBlinkStepMs = 0;
unsigned long lastStatePublishMs = 0;

const char *modeToString(AirportMode mode) {
  return mode == AirportMode::On ? "ON" : "OFF";
}

AirportMode parseMode(const String &mode) {
  return mode == "ON" ? AirportMode::On : AirportMode::Off;
}

void setAllLights(bool enabled) {
  lightsOn = enabled;
  for (size_t index = 0; index < LIGHT_COUNT; index++) {
    digitalWrite(LIGHT_PINS[index], enabled ? HIGH : LOW);
  }
}

void setActiveLights(int startIndex, int endIndexInclusive) {
  bool anyLightActive = false;
  for (size_t index = 0; index < LIGHT_COUNT; index++) {
    const bool active = static_cast<int>(index) >= startIndex && static_cast<int>(index) <= endIndexInclusive;
    digitalWrite(LIGHT_PINS[index], active ? HIGH : LOW);
    anyLightActive = anyLightActive || active;
  }
  lightsOn = anyLightActive;
}

float readDistanceCm() {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  const long duration = pulseIn(ECHO_PIN, HIGH, SENSOR_TIMEOUT_US);
  if (duration == 0) {
    return 999.0F;
  }

  return static_cast<float>(duration) * 0.034F / 2.0F;
}

void publishState(bool retained) {
  StaticJsonDocument<96> document;
  document["mode"] = modeToString(currentMode);
  document["lightsOn"] = lightsOn;
  document["online"] = true;

  char buffer[96];
  const size_t length = serializeJson(document, buffer);
  mqttClient.publish(TOPIC_STATE, reinterpret_cast<const uint8_t *>(buffer), length, retained);
  lastStatePublishMs = millis();
}

void applyMode(AirportMode mode) {
  currentMode = mode;
  blinkIndex = static_cast<int>(LIGHT_COUNT) - 1;
  lastBlinkStepMs = 0;

  if (currentMode == AirportMode::Off) {
    setAllLights(false);
  }

  publishState(true);
}

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
  if (String(topic) != TOPIC_COMMAND) {
    return;
  }

  StaticJsonDocument<128> document;
  const DeserializationError error = deserializeJson(document, payload, length);
  if (error) {
    return;
  }

  const char *action = document["action"] | "";
  if (strcmp(action, COMMAND_ACTION) != 0) {
    return;
  }

  applyMode(parseMode(document["mode"] | "OFF"));
}

void ensureMqttConnected() {
  if (mqttClient.connected()) {
    return;
  }

  while (!mqttClient.connected()) {
    if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
      mqttClient.subscribe(TOPIC_COMMAND);
      publishState(true);
      return;
    }

    delay(2000);
  }
}

void processConnectivityAndMessages() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();
}

void publishHeartbeatIfDue() {
  const unsigned long now = millis();
  if (now - lastStatePublishMs < STATE_INTERVAL_MS) {
    return;
  }

  publishState(true);
}

void runBlinkStepIfDue() {
  const unsigned long now = millis();
  if (now - lastBlinkStepMs < BLINK_INTERVAL_MS) {
    return;
  }

  lastBlinkStepMs = now;
  const int currentIndex = blinkIndex;
  setActiveLights(currentIndex, currentIndex);

  blinkIndex--;
  if (blinkIndex <= 0) {
    blinkIndex = static_cast<int>(LIGHT_COUNT) - 1;
  }
}

void trackAircraftWhileDetected() {
  while (currentMode == AirportMode::On) {
    processConnectivityAndMessages();

    const float distance = readDistanceCm();
    if (distance <= 0.0F || distance > MAX_TRACK_DISTANCE_CM) {
      break;
    }

    if (distance < 6.0F) {
      setActiveLights(0, 0);
    } else {
      int startIndex = static_cast<int>((distance - 4.0F) / 4.0F);
      if (startIndex < 0) {
        startIndex = 0;
      }
      if (startIndex >= static_cast<int>(LIGHT_COUNT)) {
        startIndex = static_cast<int>(LIGHT_COUNT) - 1;
      }

      setActiveLights(startIndex, startIndex + 2);
    }

    publishHeartbeatIfDue();
    delay(TRACK_INTERVAL_MS);
  }
}
}  // namespace

void setup() {
  Serial.begin(115200);

  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  for (size_t index = 0; index < LIGHT_COUNT; index++) {
    pinMode(LIGHT_PINS[index], OUTPUT);
  }

  setAllLights(false);
  ensureWifiConnected();

  mqttClient.setBufferSize(MQTT_BUFFER_SIZE);
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);

  ensureMqttConnected();
}

void loop() {
  processConnectivityAndMessages();

  if (currentMode == AirportMode::Off) {
    setAllLights(false);
    publishHeartbeatIfDue();
    delay(50);
    return;
  }

  const float distance = readDistanceCm();
  if (distance > MIN_TRACK_DISTANCE_CM && distance < MAX_TRACK_DISTANCE_CM) {
    trackAircraftWhileDetected();
  } else {
    runBlinkStepIfDue();
  }

  publishHeartbeatIfDue();
}
