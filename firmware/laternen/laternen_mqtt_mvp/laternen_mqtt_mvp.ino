#include <Adafruit_PWMServoDriver.h>
#include <ArduinoJson.h>
#include <BH1750.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <Wire.h>

#include "secrets.h"

// Minimaler MQTT-MVP fuer die Laternen: liest Lux, steuert Licht und synchronisiert Zustand ueber Wi-Fi.
namespace {
constexpr char TOPIC_COMMAND[] = "smartown/lanterns/command";
constexpr char TOPIC_STATE[] = "smartown/lanterns/state";
constexpr char TOPIC_EVENT[] = "smartown/lanterns/event";
constexpr uint16_t I2C_SDA_PIN = 21;
constexpr uint16_t I2C_SCL_PIN = 22;
constexpr uint16_t PWM_ON_VALUE = 4095;
constexpr uint16_t PWM_OFF_VALUE = 0;
constexpr size_t LANTERN_CHANNEL_COUNT = 16;
constexpr unsigned long STATE_INTERVAL_MS = 5000;
constexpr unsigned long SENSOR_INTERVAL_MS = 500;
constexpr size_t MQTT_BUFFER_SIZE = 512;

enum class Mode {
  Auto,
  ForcedOn,
  ForcedOff,
};

enum class LightOutput {
  On,
  Off,
};

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver(0x40);
BH1750 lightMeter;

Mode currentMode = Mode::Auto;
LightOutput currentLightOutput = LightOutput::Off;
float lastLux = 0.0f;
unsigned long lastStatePublishMs = 0;
unsigned long lastSensorReadMs = 0;

// Uebersetzt den internen Modus in das dokumentierte MQTT-Format.
const char *modeToString(Mode mode) {
  switch (mode) {
    case Mode::Auto:
      return "AUTO";
    case Mode::ForcedOn:
      return "FORCED_ON";
    case Mode::ForcedOff:
      return "FORCED_OFF";
  }

  return "AUTO";
}

// Uebersetzt den Schaltzustand in den Payload-Wert fuer MQTT.
const char *lightOutputToString(LightOutput lightOutput) {
  return lightOutput == LightOutput::On ? "ON" : "OFF";
}

// Leitet aus Modus und Luxwert einen fachlichen Grund fuer das Event ab.
const char *reasonToString(bool lightOn, Mode mode, float lux) {
  if (mode == Mode::ForcedOn || mode == Mode::ForcedOff) {
    return "MANUAL_OVERRIDE";
  }

  if (lightOn && lux < THRESHOLD_LUX) {
    return "LOW_LUX";
  }

  return "HIGH_LUX";
}

// Wandelt das empfangene MQTT-Command in den internen Betriebsmodus um.
Mode parseMode(const String &mode) {
  if (mode == "FORCED_ON") {
    return Mode::ForcedOn;
  }
  if (mode == "FORCED_OFF") {
    return Mode::ForcedOff;
  }

  return Mode::Auto;
}

// Schreibt den gewuenschten Schaltzustand auf alle PWM-Kanaele der Laternen.
void setLanternOutputs(LightOutput lightOutput) {
  const uint16_t pwmValue = lightOutput == LightOutput::On ? PWM_ON_VALUE : PWM_OFF_VALUE;
  for (size_t channel = 0; channel < LANTERN_CHANNEL_COUNT; ++channel) {
    pwm.setPWM(channel, 0, pwmValue);
  }
}

// Berechnet den Sollzustand der Lampen aus Modus und Helligkeit.
LightOutput resolveLightOutput(Mode mode, float lux) {
  if (mode == Mode::ForcedOn) {
    return LightOutput::On;
  }
  if (mode == Mode::ForcedOff) {
    return LightOutput::Off;
  }

  return lux < THRESHOLD_LUX ? LightOutput::On : LightOutput::Off;
}

// Publiziert den kompletten Status als retained MQTT-State.
void publishState(bool retained) {
  StaticJsonDocument<256> document;
  document["mode"] = modeToString(currentMode);
  document["lightState"] = lightOutputToString(currentLightOutput);
  document["lux"] = lastLux;
  document["online"] = true;
  document["thresholdLux"] = THRESHOLD_LUX;

  char buffer[256];
  const size_t length = serializeJson(document, buffer);
  mqttClient.publish(TOPIC_STATE, reinterpret_cast<const uint8_t *>(buffer), length, retained);
}

// Publiziert ein einzelnes Fach-Event ohne Retain-Flag.
void publishEvent(const char *type, const char *reason) {
  StaticJsonDocument<192> document;
  document["type"] = type;
  document["lightState"] = lightOutputToString(currentLightOutput);
  document["reason"] = reason;

  char buffer[192];
  const size_t length = serializeJson(document, buffer);
  mqttClient.publish(TOPIC_EVENT, reinterpret_cast<const uint8_t *>(buffer), length, false);
}

// Wendet den aktuellen Sollzustand an, publiziert State und bei Bedarf ein Event.
void applyCurrentState(bool emitEvent, const char *eventType, const char *reason) {
  const LightOutput nextLightOutput = resolveLightOutput(currentMode, lastLux);
  const bool changed = nextLightOutput != currentLightOutput;

  currentLightOutput = nextLightOutput;
  setLanternOutputs(currentLightOutput);
  publishState(true);

  if (emitEvent || changed) {
    publishEvent(eventType, reason);
  }
}

// Baut bei Bedarf die Wi-Fi-Verbindung ins Schulnetz wieder auf.
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

// Verarbeitet eingehende MQTT-Kommandos und aktualisiert danach den Lichtzustand.
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
  applyCurrentState(true, "LIGHT_STATE_CHANGED", reasonToString(currentLightOutput == LightOutput::On, currentMode, lastLux));
}

// Baut die MQTT-Verbindung auf, abonniert Commands und sendet den Initialzustand.
void ensureMqttConnected() {
  if (mqttClient.connected()) {
    return;
  }

  while (!mqttClient.connected()) {
    if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
      mqttClient.subscribe(TOPIC_COMMAND);
      publishState(true);
      publishEvent("SYSTEM_START", "SYSTEM_START");
      return;
    }

    delay(2000);
  }
}

// Liest den BH1750 in festen Intervallen und reagiert im AUTO-Modus auf Aenderungen.
void readSensorIfDue() {
  const unsigned long now = millis();
  if (now - lastSensorReadMs < SENSOR_INTERVAL_MS) {
    return;
  }

  lastSensorReadMs = now;
  lastLux = lightMeter.readLightLevel();

  if (currentMode == Mode::Auto) {
    const LightOutput previousLightOutput = currentLightOutput;
    applyCurrentState(false, "LIGHT_STATE_CHANGED", reasonToString(currentLightOutput == LightOutput::On, currentMode, lastLux));

    if (previousLightOutput != currentLightOutput) {
      publishEvent("LIGHT_STATE_CHANGED", reasonToString(currentLightOutput == LightOutput::On, currentMode, lastLux));
    }
  }
}

// Sendet regelmaessig einen retained Heartbeat mit dem aktuellen State.
void publishHeartbeatIfDue() {
  const unsigned long now = millis();
  if (now - lastStatePublishMs < STATE_INTERVAL_MS) {
    return;
  }

  lastStatePublishMs = now;
  publishState(true);
}
}  // namespace

// Initialisiert Busse, Sensoren, Ausgaenge und die erste Netzwerkverbindung.
void setup() {
  Serial.begin(115200);

  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
  pwm.begin();
  pwm.setPWMFreq(1000);
  lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE);

  ensureWifiConnected();

  mqttClient.setBufferSize(MQTT_BUFFER_SIZE);
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);

  lastLux = lightMeter.readLightLevel();
  currentLightOutput = resolveLightOutput(currentMode, lastLux);
  setLanternOutputs(currentLightOutput);

  ensureMqttConnected();
}

// Haelt Netzwerk, MQTT und Sensorik waehrend der gesamten Laufzeit synchron.
void loop() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();
  readSensorIfDue();
  publishHeartbeatIfDue();
}
