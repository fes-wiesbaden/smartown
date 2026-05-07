#include <PubSubClient.h>
#include <Stepper.h>
#include <WiFi.h>

#include "secrets.h"

namespace {
constexpr int STEPS_PER_REVOLUTION = 2048;
constexpr int BRIDGE_TRAVEL_STEPS = 340;
constexpr int BRIDGE_MOTOR_SPEED_RPM = 3;
constexpr unsigned long ECHO_TIMEOUT_US = 30000;
constexpr unsigned long SENSOR_SETTLE_DELAY_MS = 50;
constexpr unsigned long LOOP_PAUSE_MS = 100;
constexpr unsigned long HEARTBEAT_INTERVAL_MS = 10000;
constexpr unsigned long EVENT_COOLDOWN_MS = 2000;

// 28BYJ-48 mit ULN2003. Reihenfolge im Stepper-Konstruktor ist für diesen Treiber wichtig.
constexpr uint8_t MOTOR_IN1_PIN = 13;
constexpr uint8_t MOTOR_IN2_PIN = 12;
constexpr uint8_t MOTOR_IN3_PIN = 14;
constexpr uint8_t MOTOR_IN4_PIN = 27;

// Zwei HC-SR04-Sensoren erkennen die Fahrtrichtung über die Reihenfolge der Events.
constexpr uint8_t FIRST_SENSOR_TRIGGER_PIN = 32;
constexpr uint8_t FIRST_SENSOR_ECHO_PIN = 33;
constexpr uint8_t SECOND_SENSOR_TRIGGER_PIN = 25;
constexpr uint8_t SECOND_SENSOR_ECHO_PIN = 26;

constexpr char MQTT_TOPIC_EVENT[] = "smartown/bridge/event";
constexpr char MQTT_TOPIC_COMMAND[] = "smartown/bridge/command";
constexpr char MQTT_TOPIC_STATE[] = "smartown/bridge/state";
constexpr char COMMAND_OPEN[] = "OPEN";
constexpr char COMMAND_CLOSE[] = "CLOSE";
constexpr char STATE_ONLINE[] = "ONLINE";
constexpr char EVENT_FIRST_SENSOR[] = "BOAT_DETECTED_SENSOR_1";
constexpr char EVENT_SECOND_SENSOR[] = "BOAT_DETECTED_SENSOR_2";

constexpr float MIN_DETECTION_DISTANCE_CM = 0.0F;
constexpr float MAX_DETECTION_DISTANCE_CM = 4.0F;
constexpr float INVALID_DISTANCE_CM = 999.0F;

Stepper bridgeMotor(STEPS_PER_REVOLUTION, MOTOR_IN1_PIN, MOTOR_IN3_PIN, MOTOR_IN2_PIN, MOTOR_IN4_PIN);

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

bool firstSensorOccupied = false;
bool secondSensorOccupied = false;
unsigned long lastBridgeEventMs = 0;
unsigned long lastHeartbeatPublishMs = 0;
unsigned long lastDistanceLogMs = 0;

// Baut die Wi-Fi-Verbindung bei Bedarf wieder auf.
void ensureWifiConnected() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }

  Serial.print("WLAN-Connect zu: ");
  Serial.println(WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWLAN verbunden!");
}

void moveBridge(int steps) {
  bridgeMotor.setSpeed(BRIDGE_MOTOR_SPEED_RPM);
  bridgeMotor.step(steps);
}

void openBridge() {
  moveBridge(BRIDGE_TRAVEL_STEPS);
}

void closeBridge() {
  moveBridge(-BRIDGE_TRAVEL_STEPS);
}

// Verarbeitet die einfachen Backend-Kommandos "OPEN" und "CLOSE".
void handleCommand(char *topic, byte *payload, unsigned int length) {
  if (String(topic) != MQTT_TOPIC_COMMAND) {
    return;
  }

  String command;
  command.reserve(length);
  for (unsigned int i = 0; i < length; ++i) {
    command += static_cast<char>(payload[i]);
  }
  command.trim();

  Serial.print("MQTT Befehl: ");
  Serial.println(command);

  if (command == COMMAND_OPEN) {
    openBridge();
  } else if (command == COMMAND_CLOSE) {
    closeBridge();
  }
}

// Verbindet sich mit dem Broker und abonniert Brücken-Kommandos.
void ensureMqttConnected() {
  while (!mqttClient.connected()) {
    Serial.print("MQTT-Connect (");
    Serial.print(MQTT_HOST);
    Serial.print(")... ");
    if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
      Serial.println("OK!");
      mqttClient.subscribe(MQTT_TOPIC_COMMAND);
    } else {
      Serial.print("Fail, Code=");
      Serial.print(mqttClient.state());
      delay(2000);
    }
  }
}

float readDistanceCm(uint8_t triggerPin, uint8_t echoPin) {
  digitalWrite(triggerPin, LOW);
  delayMicroseconds(2);
  digitalWrite(triggerPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(triggerPin, LOW);

  const unsigned long echoDurationUs = pulseIn(echoPin, HIGH, ECHO_TIMEOUT_US);
  if (echoDurationUs == 0) {
    return INVALID_DISTANCE_CM;
  }

  return echoDurationUs * 0.034F / 2.0F;
}

bool isBoatInDetectionRange(float distanceCm) {
  return distanceCm >= MIN_DETECTION_DISTANCE_CM && distanceCm <= MAX_DETECTION_DISTANCE_CM;
}

void publishHeartbeatIfDue() {
  const unsigned long now = millis();
  if (now - lastHeartbeatPublishMs <= HEARTBEAT_INTERVAL_MS) {
    return;
  }

  mqttClient.publish(MQTT_TOPIC_STATE, STATE_ONLINE);
  lastHeartbeatPublishMs = now;
}

void logDistancesIfDue(float firstSensorDistanceCm, float secondSensorDistanceCm) {
  const unsigned long now = millis();
  if (now - lastDistanceLogMs <= 1000) {
    return;
  }

  Serial.print("S1: ");
  Serial.print(firstSensorDistanceCm);
  Serial.print(" cm | S2: ");
  Serial.print(secondSensorDistanceCm);
  Serial.println(" cm");
  lastDistanceLogMs = now;
}

// Sendet ein Event nur beim Eintritt in den Erfassungsbereich und mit Cooldown gegen Flattern.
void publishSensorEventIfTriggered(
    float distanceCm,
    bool &sensorOccupied,
    const char *eventPayload,
    const char *logMessage) {
  if (!isBoatInDetectionRange(distanceCm)) {
    sensorOccupied = false;
    return;
  }

  const unsigned long now = millis();
  if (!sensorOccupied && (now - lastBridgeEventMs > EVENT_COOLDOWN_MS)) {
    Serial.println(logMessage);
    mqttClient.publish(MQTT_TOPIC_EVENT, eventPayload);
    lastBridgeEventMs = now;
    sensorOccupied = true;
  }
}
}  // namespace

// Initialisiert Sensorpins, Wi-Fi und MQTT.
void setup() {
  Serial.begin(115200);
  pinMode(FIRST_SENSOR_TRIGGER_PIN, OUTPUT);
  pinMode(FIRST_SENSOR_ECHO_PIN, INPUT);
  pinMode(SECOND_SENSOR_TRIGGER_PIN, OUTPUT);
  pinMode(SECOND_SENSOR_ECHO_PIN, INPUT);

  Serial.println("\n--- BRUECKENSTEUERUNG GESTARTET ---");

  ensureWifiConnected();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);
  ensureMqttConnected();
}

// Hält Netzwerk/MQTT aktiv, misst beide Sensoren und publiziert Bootserkennung.
void loop() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();

  publishHeartbeatIfDue();

  const float firstSensorDistanceCm = readDistanceCm(FIRST_SENSOR_TRIGGER_PIN, FIRST_SENSOR_ECHO_PIN);
  delay(SENSOR_SETTLE_DELAY_MS);
  const float secondSensorDistanceCm = readDistanceCm(SECOND_SENSOR_TRIGGER_PIN, SECOND_SENSOR_ECHO_PIN);

  logDistancesIfDue(firstSensorDistanceCm, secondSensorDistanceCm);
  publishSensorEventIfTriggered(
      firstSensorDistanceCm,
      firstSensorOccupied,
      EVENT_FIRST_SENSOR,
      ">>> EVENT: Sensor 1 Trigger!");
  publishSensorEventIfTriggered(
      secondSensorDistanceCm,
      secondSensorOccupied,
      EVENT_SECOND_SENSOR,
      ">>> EVENT: Sensor 2 Trigger!");

  delay(LOOP_PAUSE_MS);
}
