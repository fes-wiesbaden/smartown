#include <ArduinoJson.h>
#include <PubSubClient.h>
#include <WiFi.h>

#include "secrets.h"

// Schlanker MQTT-Testsketch fuer genau eine Laterne an GPIO32 ohne Helligkeitssensor.
namespace {
constexpr char TOPIC_COMMAND[] = "smartown/lanterns/command";
constexpr char TOPIC_STATE[] = "smartown/lanterns/state";
constexpr char TOPIC_EVENT[] = "smartown/lanterns/event";
constexpr uint8_t LANTERN_PIN = 32;
constexpr unsigned long STATE_INTERVAL_MS = 5000;
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

Mode currentMode = Mode::Auto;
LightOutput currentLightOutput = LightOutput::Off;
unsigned long lastStatePublishMs = 0;

// Uebersetzt den internen Modus in das MQTT-Schema des Projekts.
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

// Uebersetzt den Lampenzustand in den MQTT-Wert.
const char *lightOutputToString(LightOutput lightOutput) {
  return lightOutput == LightOutput::On ? "ON" : "OFF";
}

// Dieser Testsketch kennt keinen Sensor. AUTO bleibt daher aus Kompatibilitaetsgruenden erhalten und mappt auf AUS.
LightOutput resolveLightOutput(Mode mode) {
  if (mode == Mode::ForcedOn) {
    return LightOutput::On;
  }

  return LightOutput::Off;
}

// Wandelt das empfangene Command in den internen Modus um.
Mode parseMode(const String &mode) {
  if (mode == "FORCED_ON") {
    return Mode::ForcedOn;
  }
  if (mode == "FORCED_OFF") {
    return Mode::ForcedOff;
  }

  return Mode::Auto;
}

// Schreibt den gewuenschten Zustand direkt auf GPIO32.
void setLanternOutput(LightOutput lightOutput) {
  digitalWrite(LANTERN_PIN, lightOutput == LightOutput::On ? HIGH : LOW);
}

// Publiziert den aktuellen Zustand als retained State-Nachricht.
void publishState(bool retained) {
  StaticJsonDocument<192> document;
  document["mode"] = modeToString(currentMode);
  document["lightState"] = lightOutputToString(currentLightOutput);
  document["lux"] = nullptr;
  document["online"] = true;
  document["thresholdLux"] = nullptr;

  char buffer[192];
  const size_t length = serializeJson(document, buffer);
  mqttClient.publish(TOPIC_STATE, reinterpret_cast<const uint8_t *>(buffer), length, retained);
}

// Publiziert Ereignisse fuer Start und manuelle Moduswechsel.
void publishEvent(const char *type, const char *reason) {
  StaticJsonDocument<192> document;
  document["type"] = type;
  document["lightState"] = lightOutputToString(currentLightOutput);
  document["reason"] = reason;

  char buffer[192];
  const size_t length = serializeJson(document, buffer);
  mqttClient.publish(TOPIC_EVENT, reinterpret_cast<const uint8_t *>(buffer), length, false);
}

// Wendet den aktuellen Modus an und synchronisiert Ausgang und MQTT-Status.
void applyCurrentState(bool emitEvent, const char *eventType, const char *reason) {
  currentLightOutput = resolveLightOutput(currentMode);
  setLanternOutput(currentLightOutput);
  publishState(true);

  if (emitEvent) {
    publishEvent(eventType, reason);
  }
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

// Verarbeitet eingehende MQTT-Kommandos fuer den Lampenmodus.
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
  applyCurrentState(true, "LIGHT_STATE_CHANGED", "MANUAL_OVERRIDE");
}

// Baut die Broker-Verbindung auf, abonniert Commands und sendet den Initialzustand.
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

// Sendet regelmaessig den aktuellen Zustand erneut, damit Backend und Frontend synchron bleiben.
void publishHeartbeatIfDue() {
  const unsigned long now = millis();
  if (now - lastStatePublishMs < STATE_INTERVAL_MS) {
    return;
  }

  lastStatePublishMs = now;
  publishState(true);
}
}  // namespace

// Initialisiert GPIO, Wi-Fi und MQTT fuer den Einzel-Laternen-Test.
void setup() {
  Serial.begin(115200);

  pinMode(LANTERN_PIN, OUTPUT);
  setLanternOutput(LightOutput::Off);

  ensureWifiConnected();

  mqttClient.setBufferSize(MQTT_BUFFER_SIZE);
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);

  applyCurrentState(false, "SYSTEM_START", "SYSTEM_START");
  ensureMqttConnected();
}

// Haelt Netzwerk und MQTT dauerhaft aktiv.
void loop() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();
  publishHeartbeatIfDue();
}
