#include <WiFi.h>
#include <PubSubClient.h>
#include <Stepper.h>

#include "secrets.h"

const int STEPS_PER_REVOLUTION = 2048;
const int STEP_ANGLE = 300;
const int MOTOR_SPEED_RPM = 3;
const unsigned long SENSOR_TIMEOUT_US = 30000;
const unsigned long SENSOR_SETTLE_DELAY_MS = 30;
const unsigned long LOOP_DELAY_MS = 50;

const int TRIG_1_PIN = 2;
const int ECHO_1_PIN = 3;
const int TRIG_2_PIN = 4;
const int ECHO_2_PIN = 5;

const float MIN_DISTANCE_CM = 1.0;
const float MAX_DISTANCE_CM = 4.5;

Stepper bridgeMotor(STEPS_PER_REVOLUTION, 8, 10, 9, 11);

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

unsigned long lastEventMs = 0;
const unsigned long EVENT_COOLDOWN_MS = 3000;

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

  if (message.indexOf("OPEN") != -1) {
    Serial.println("MQTT Command: OPEN -> Faehre Bruecke hoch (Vorwaerts)");
    bridgeMotor.setSpeed(MOTOR_SPEED_RPM);
    bridgeMotor.step(STEP_ANGLE);
  } else if (message.indexOf("CLOSE") != -1) {
    Serial.println("MQTT Command: CLOSE -> Faehre Bruecke runter (Rueckwaerts)");
    bridgeMotor.setSpeed(MOTOR_SPEED_RPM);
    bridgeMotor.step(-STEP_ANGLE);
  }
}

void ensureMqttConnected() {
  while (!mqttClient.connected()) {
    if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
      mqttClient.subscribe(MQTT_TOPIC_COMMAND);
      return;
    }
    delay(2000);
  }
}

float readDistanceCm(int trigPin, int echoPin) {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  const long duration = pulseIn(echoPin, HIGH, SENSOR_TIMEOUT_US);
  if (duration == 0) {
    return 999.0;
  }
  return duration * 0.034f / 2.0f;
}

bool isInDetectionRange(float distanceCm) {
  return distanceCm >= MIN_DISTANCE_CM && distanceCm <= MAX_DISTANCE_CM;
}

void setup() {
  pinMode(TRIG_1_PIN, OUTPUT);
  pinMode(ECHO_1_PIN, INPUT);
  pinMode(TRIG_2_PIN, OUTPUT);
  pinMode(ECHO_2_PIN, INPUT);
  Serial.begin(9600);
  
  ensureWifiConnected();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);
  ensureMqttConnected();
}

void loop() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();

  // Vermeide spammen von Events an den Broker
  if (millis() - lastEventMs < EVENT_COOLDOWN_MS) {
    return;
  }

  const float distance1 = readDistanceCm(TRIG_1_PIN, ECHO_1_PIN);
  delay(SENSOR_SETTLE_DELAY_MS);
  const float distance2 = readDistanceCm(TRIG_2_PIN, ECHO_2_PIN);

  const bool sensor1Active = isInDetectionRange(distance1);
  const bool sensor2Active = isInDetectionRange(distance2);

  if (sensor1Active) {
    Serial.println("Sensor 1 (vor der Bruecke) ausgeloest -> Sende MQTT Event");
    mqttClient.publish(MQTT_TOPIC_EVENT, "BOAT_DETECTED_SENSOR_1");
    lastEventMs = millis();
  } else if (sensor2Active) {
    Serial.println("Sensor 2 (nach der Bruecke) ausgeloest -> Sende MQTT Event");
    mqttClient.publish(MQTT_TOPIC_EVENT, "BOAT_DETECTED_SENSOR_2");
    lastEventMs = millis();
  }

  delay(LOOP_DELAY_MS);
}
