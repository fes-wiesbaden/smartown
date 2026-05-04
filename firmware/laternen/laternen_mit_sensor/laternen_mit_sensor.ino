#include <Adafruit_PWMServoDriver.h>
#include <ArduinoJson.h>
#include <BH1750.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <Wire.h>

#include "../laterne_mqtt_d32/secrets.h"

namespace {
constexpr char TOPIC_COMMAND[] = "smartown/lanterns/command";
constexpr char TOPIC_STATE[] = "smartown/lanterns/state";
constexpr char TOPIC_EVENT[] = "smartown/lanterns/event";
constexpr char MQTT_CLIENT_ID_RUNTIME[] = "esp32-lantern-mqtt";
constexpr float THRESHOLD_LUX = 50.0F;
constexpr uint8_t I2C_SDA_PIN = 21;
constexpr uint8_t I2C_SCL_PIN = 22;
constexpr uint8_t PWM_DRIVER_ADDRESS = 0x40;
constexpr uint8_t FIRST_LANTERN_CHANNEL = 0;
constexpr uint8_t LAST_LANTERN_CHANNEL = 15;
constexpr uint16_t PWM_FREQUENCY = 1000;
constexpr unsigned long SENSOR_INTERVAL_MS = 500;
constexpr unsigned long STATE_INTERVAL_MS = 5000;
constexpr size_t MQTT_BUFFER_SIZE = 512;

enum class LanternMode {
  Auto,
  On,
  Off,
};

enum class LightState {
  On,
  Off,
};

Adafruit_PWMServoDriver pwm(PWM_DRIVER_ADDRESS);
BH1750 lightMeter;
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

LanternMode currentMode = LanternMode::Auto;
LightState currentLightState = LightState::Off;
float lastLux = NAN;
unsigned long lastSensorReadMs = 0;
unsigned long lastStatePublishMs = 0;

const char *modeToString(LanternMode mode) {
  switch (mode) {
    case LanternMode::Auto:
      return "AUTO";
    case LanternMode::On:
      return "ON";
    case LanternMode::Off:
      return "OFF";
  }

  return "AUTO";
}

const char *lightStateToString(LightState lightState) {
  return lightState == LightState::On ? "ON" : "OFF";
}

const char *reasonForLux(float lux) {
  return lux < THRESHOLD_LUX ? "LOW_LUX" : "HIGH_LUX";
}

LanternMode parseMode(const String &mode) {
  if (mode == "ON") {
    return LanternMode::On;
  }
  if (mode == "OFF") {
    return LanternMode::Off;
  }

  return LanternMode::Auto;
}

bool isValidLux(float lux) {
  return !isnan(lux) && lux >= 0.0F;
}

void setAllLanternChannels(bool enabled) {
  // Die PCA9685-Kanaele werden gemeinsam geschaltet, weil alle Laternen denselben Zustand teilen.
  for (uint8_t channel = FIRST_LANTERN_CHANNEL; channel <= LAST_LANTERN_CHANNEL; channel++) {
    pwm.setPWM(channel, 0, enabled ? 0 : 4095);
  }
}

LightState resolveLightStateForMode(LanternMode mode, float lux, bool hasLux, LightState fallback) {
  if (mode == LanternMode::On) {
    return LightState::On;
  }
  if (mode == LanternMode::Off) {
    return LightState::Off;
  }
  if (!hasLux) {
    return fallback;
  }

  return lux < THRESHOLD_LUX ? LightState::On : LightState::Off;
}

void publishState(bool retained) {
  StaticJsonDocument<192> document;
  document["mode"] = modeToString(currentMode);
  document["lightState"] = lightStateToString(currentLightState);
  if (isValidLux(lastLux)) {
    document["lux"] = lastLux;
  } else {
    document["lux"] = nullptr;
  }
  document["online"] = true;
  document["thresholdLux"] = THRESHOLD_LUX;

  char buffer[192];
  const size_t length = serializeJson(document, buffer);
  mqttClient.publish(TOPIC_STATE, reinterpret_cast<const uint8_t *>(buffer), length, retained);
  lastStatePublishMs = millis();
}

void publishEvent(const char *type, const char *reason) {
  StaticJsonDocument<160> document;
  document["type"] = type;
  document["lightState"] = lightStateToString(currentLightState);
  document["reason"] = reason;

  char buffer[160];
  const size_t length = serializeJson(document, buffer);
  mqttClient.publish(TOPIC_EVENT, reinterpret_cast<const uint8_t *>(buffer), length, false);
}

void applyHardwareState(LightState nextLightState) {
  currentLightState = nextLightState;
  setAllLanternChannels(nextLightState == LightState::On);
}

void updateLanternState(bool emitEvent, const char *eventType, const char *reason) {
  const bool hasLux = isValidLux(lastLux);
  const LightState nextLightState = resolveLightStateForMode(currentMode, lastLux, hasLux, currentLightState);
  const bool lightChanged = nextLightState != currentLightState;

  if (lightChanged) {
    applyHardwareState(nextLightState);
  }

  publishState(true);

  if (!emitEvent) {
    return;
  }

  if (eventType != nullptr && reason != nullptr) {
    publishEvent(eventType, reason);
    return;
  }

  if (lightChanged && currentMode == LanternMode::Auto && hasLux) {
    publishEvent("LIGHT_STATE_CHANGED", reasonForLux(lastLux));
  }
}

bool refreshLux() {
  const float lux = lightMeter.readLightLevel();
  if (!isValidLux(lux)) {
    lastLux = NAN;
    return false;
  }

  lastLux = lux;
  return true;
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

void resetToAutoAfterReconnect() {
  // Jeder Broker-Reconnect startet bewusst wieder im Grundmodus AUTO.
  currentMode = LanternMode::Auto;
  refreshLux();
  updateLanternState(false, nullptr, nullptr);
}

void handleCommand(char *topic, byte *payload, unsigned int length) {
  if (String(topic) != TOPIC_COMMAND) {
    return;
  }

  StaticJsonDocument<192> document;
  DeserializationError error = deserializeJson(document, payload, length);
  if (error) {
    return;
  }

  const String action = document["action"] | "";
  if (action != "SET_MODE") {
    return;
  }

  currentMode = parseMode(document["mode"] | "AUTO");
  refreshLux();
  updateLanternState(true, "MODE_CHANGED", "MANUAL_OVERRIDE");
}

void ensureMqttConnected() {
  if (mqttClient.connected()) {
    return;
  }

  while (!mqttClient.connected()) {
    if (mqttClient.connect(MQTT_CLIENT_ID_RUNTIME, MQTT_USERNAME, MQTT_PASSWORD)) {
      mqttClient.subscribe(TOPIC_COMMAND);
      resetToAutoAfterReconnect();
      publishEvent("SYSTEM_START", "SYSTEM_START");
      return;
    }

    delay(2000);
  }
}

void updateAutoModeIfDue() {
  const unsigned long now = millis();
  if (now - lastSensorReadMs < SENSOR_INTERVAL_MS) {
    return;
  }

  lastSensorReadMs = now;
  const LightState previousLightState = currentLightState;
  const bool hasLux = refreshLux();

  if (currentMode != LanternMode::Auto || !hasLux) {
    return;
  }

  updateLanternState(false, nullptr, nullptr);

  if (currentLightState != previousLightState) {
    Serial.print("AUTO ");
    Serial.print(lastLux);
    Serial.print(" lx -> ");
    Serial.println(lightStateToString(currentLightState));
  }
}

void publishHeartbeatIfDue() {
  const unsigned long now = millis();
  if (now - lastStatePublishMs < STATE_INTERVAL_MS) {
    return;
  }

  refreshLux();
  publishState(true);
}
}  // namespace

void setup() {
  Serial.begin(115200);
  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);

  pwm.begin();
  pwm.setPWMFreq(PWM_FREQUENCY);
  setAllLanternChannels(false);

  lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE);

  ensureWifiConnected();

  mqttClient.setBufferSize(MQTT_BUFFER_SIZE);
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);

  refreshLux();
  updateLanternState(false, nullptr, nullptr);
  ensureMqttConnected();
}

void loop() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();
  updateAutoModeIfDue();
  publishHeartbeatIfDue();
}
