#include <Adafruit_PWMServoDriver.h>
#include <ArduinoJson.h>
#include <BH1750.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <Wire.h>

#include "../laternen_mit_sensor/secrets.h"

namespace {
constexpr char TOPIC_COMMAND[] = "smartown/lanterns/command";
constexpr char TOPIC_STATE[] = "smartown/lanterns/state";
constexpr char TOPIC_EVENT[] = "smartown/lanterns/event";
constexpr float DARKNESS_THRESHOLD_LUX = 100.0F;
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
float lastMeasuredLux = NAN;
unsigned long lastSensorReadMs = 0;
unsigned long lastStatePublishMs = 0;
char mqttClientId[32] = "";
bool startupEventPublished = false;

// Übersetzt den internen Modus in das MQTT-Schema des Projekts.
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

// Übersetzt den physischen Lampenzustand in den MQTT-Wert.
const char *lightStateToString(LightState lightState) {
  return lightState == LightState::On ? "ON" : "OFF";
}

const char *reasonForLux(float lux) {
  return lux < DARKNESS_THRESHOLD_LUX ? "LOW_LUX" : "HIGH_LUX";
}

// Unbekannte Werte fallen bewusst auf AUTO zurück, damit fehlerhafte Commands keinen Dauerzustand erzwingen.
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

void setLanternOutputs(bool enabled) {
  // Die PCA9685-Kanäle werden gemeinsam geschaltet, weil alle Laternen denselben Zustand teilen.
  for (uint8_t channel = FIRST_LANTERN_CHANNEL; channel <= LAST_LANTERN_CHANNEL; channel++) {
    pwm.setPWM(channel, 0, enabled ? 0 : 4095);
  }
}

LightState determineTargetLightState(LanternMode mode, float lux, bool hasLux, LightState fallback) {
  if (mode == LanternMode::On) {
    return LightState::On;
  }
  if (mode == LanternMode::Off) {
    return LightState::Off;
  }
  if (!hasLux) {
    // Ohne gültigen Sensorwert bleibt der letzte Ausgangszustand erhalten.
    return fallback;
  }

  return lux < DARKNESS_THRESHOLD_LUX ? LightState::On : LightState::Off;
}

// Publiziert den aktuellen Zustand retained, damit Backend/Frontend nach Reconnect sofort Daten sehen.
void publishState(bool retained) {
  StaticJsonDocument<192> document;
  document["mode"] = modeToString(currentMode);
  document["lightState"] = lightStateToString(currentLightState);
  if (isValidLux(lastMeasuredLux)) {
    document["lux"] = lastMeasuredLux;
  } else {
    document["lux"] = nullptr;
  }
  document["online"] = true;
  document["thresholdLux"] = DARKNESS_THRESHOLD_LUX;

  char buffer[192];
  const size_t length = serializeJson(document, buffer);
  mqttClient.publish(TOPIC_STATE, reinterpret_cast<const uint8_t *>(buffer), length, retained);
  lastStatePublishMs = millis();
}

// Ereignisse sind nicht retained: sie beschreiben nur konkrete Zustandswechsel oder Starts.
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
  setLanternOutputs(nextLightState == LightState::On);
}

// Wendet den aktuellen Modus an, schaltet bei Bedarf die Hardware und spiegelt den Zustand per MQTT.
void synchronizeLanternState(bool emitEvent, const char *eventType, const char *reason) {
  const bool hasLux = isValidLux(lastMeasuredLux);
  const LightState nextLightState =
      determineTargetLightState(currentMode, lastMeasuredLux, hasLux, currentLightState);
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
    publishEvent("LIGHT_STATE_CHANGED", reasonForLux(lastMeasuredLux));
  }
}

bool readAmbientLux() {
  const float lux = lightMeter.readLightLevel();
  if (!isValidLux(lux)) {
    lastMeasuredLux = NAN;
    return false;
  }

  lastMeasuredLux = lux;
  return true;
}

// Baut die Wi-Fi-Verbindung bei Bedarf wieder auf.
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

// Erzeugt eine stabile MQTT-Client-ID aus den letzten drei MAC-Bytes.
void ensureMqttClientIdInitialized() {
  if (mqttClientId[0] != '\0') {
    return;
  }

  uint8_t mac[6];
  WiFi.macAddress(mac);
  snprintf(
      mqttClientId,
      sizeof(mqttClientId),
      "esp32-lantern-%02X%02X%02X",
      mac[3],
      mac[4],
      mac[5]);
}

void publishCurrentStateAfterReconnect() {
  // Nach einem Broker-Reconnect bleibt der zuletzt aktive Modus erhalten.
  readAmbientLux();
  synchronizeLanternState(false, nullptr, nullptr);
}

// Verarbeitet MQTT-Kommandos zum Wechseln zwischen AUTO, ON und OFF.
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
  readAmbientLux();
  synchronizeLanternState(true, "MODE_CHANGED", "MANUAL_OVERRIDE");
}

// Verbindet sich mit dem Broker, abonniert Commands und sendet den aktuellen retained State.
void ensureMqttConnected() {
  if (mqttClient.connected()) {
    return;
  }

  ensureMqttClientIdInitialized();

  while (!mqttClient.connected()) {
    if (mqttClient.connect(mqttClientId, MQTT_USERNAME, MQTT_PASSWORD)) {
      mqttClient.subscribe(TOPIC_COMMAND);
      publishCurrentStateAfterReconnect();
      if (!startupEventPublished) {
        publishEvent("SYSTEM_START", "SYSTEM_START");
        startupEventPublished = true;
      }
      return;
    }

    delay(2000);
  }
}

void updateAutoModeFromSensorIfDue() {
  const unsigned long now = millis();
  if (now - lastSensorReadMs < SENSOR_INTERVAL_MS) {
    return;
  }

  lastSensorReadMs = now;
  const LightState previousLightState = currentLightState;
  const bool hasLux = readAmbientLux();

  if (currentMode != LanternMode::Auto || !hasLux) {
    return;
  }

  synchronizeLanternState(false, nullptr, nullptr);

  if (currentLightState != previousLightState) {
    Serial.print("AUTO ");
    Serial.print(lastMeasuredLux);
    Serial.print(" lx -> ");
    Serial.println(lightStateToString(currentLightState));
  }
}

// Sendet regelmäßig den aktuellen Zustand erneut, damit Ausfälle im Backend auffallen.
void publishHeartbeatIfDue() {
  const unsigned long now = millis();
  if (now - lastStatePublishMs < STATE_INTERVAL_MS) {
    return;
  }

  readAmbientLux();
  publishState(true);
}
}  // namespace

// Initialisiert I2C, PWM-Treiber, Lichtsensor, Wi-Fi und MQTT.
void setup() {
  Serial.begin(115200);
  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);

  pwm.begin();
  pwm.setPWMFreq(PWM_FREQUENCY);
  setLanternOutputs(false);

  lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE);

  ensureWifiConnected();

  mqttClient.setBufferSize(MQTT_BUFFER_SIZE);
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);

  readAmbientLux();
  synchronizeLanternState(false, nullptr, nullptr);
  ensureMqttConnected();
}

// Hält Netzwerk, MQTT und den AUTO-Modus dauerhaft aktiv.
void loop() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();
  updateAutoModeFromSensorIfDue();
  publishHeartbeatIfDue();
}
