#include <PubSubClient.h>
#include <Stepper.h>
#include <WiFi.h>

#include "secrets.h"

const int STEPS_PER_REVOLUTION = 2048;
const int STEP_ANGLE = 340;
const int MOTOR_SPEED_RPM = 2;
const unsigned long SENSOR_TIMEOUT_US = 30000;
const unsigned long SENSOR_SETTLE_DELAY_MS = 50; // Erhöht für Stabilität
const unsigned long LOOP_DELAY_MS = 100;

// Pins für Schrittmotor (28BYJ-48 mit ULN2003)
#define IN1 13
#define IN2 12 // User hat IN2 wieder auf 12 gesteckt!
#define IN3 14
#define IN4 27

// Pins für Ultraschallsensoren (HC-SR04)
#define TRIG_1_PIN 32
#define ECHO_1_PIN 33
#define TRIG_2_PIN 25
#define ECHO_2_PIN 26

// MQTT Topics
#define MQTT_TOPIC_EVENT "smartown/bridge/event"
#define MQTT_TOPIC_COMMAND "smartown/bridge/command"
#define MQTT_TOPIC_STATE "smartown/bridge/state"

const float MIN_DISTANCE_CM = 2.0;
const float MAX_DISTANCE_CM = 30.0; // Reichweite erhöht für leichteres Testen

Stepper bridgeMotor(STEPS_PER_REVOLUTION, IN1, IN3, IN2, IN4);

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

bool sensor1Active = false;
bool sensor2Active = false;
unsigned long lastEventMs = 0;
const unsigned long EVENT_COOLDOWN_MS = 2000;

void ensureWifiConnected() {
  if (WiFi.status() == WL_CONNECTED)
    return;
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

void handleCommand(char *topic, byte *payload, unsigned int length) {
  String message = "";
  for (unsigned int i = 0; i < length; ++i)
    message += (char)payload[i];

  Serial.print("MQTT Befehl: ");
  Serial.println(message);

  if (message.indexOf("OPEN") != -1) {
    bridgeMotor.setSpeed(MOTOR_SPEED_RPM);
    bridgeMotor.step(STEP_ANGLE);
  } else if (message.indexOf("CLOSE") != -1) {
    bridgeMotor.setSpeed(MOTOR_SPEED_RPM);
    bridgeMotor.step(-STEP_ANGLE);
  }
}

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

float readDistanceCm(int trigPin, int echoPin) {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  long duration = pulseIn(echoPin, HIGH, SENSOR_TIMEOUT_US);
  if (duration == 0)
    return 999.0;
  return duration * 0.034 / 2.0;
}

void setup() {
  Serial.begin(115200);
  pinMode(TRIG_1_PIN, OUTPUT);
  pinMode(ECHO_1_PIN, INPUT);
  pinMode(TRIG_2_PIN, OUTPUT);
  pinMode(ECHO_2_PIN, INPUT);

  Serial.println("\n--- DIAGNOSE-MODUS AKTIVIERT ---");

  ensureWifiConnected();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);
  ensureMqttConnected();
}

void loop() {
  ensureWifiConnected();
  ensureMqttConnected();
  mqttClient.loop();

  // Heartbeat alle 10s
  static unsigned long lastHb = 0;
  if (millis() - lastHb > 10000) {
    mqttClient.publish("smartown/bridge/state", "ONLINE");
    lastHb = millis();
  }

  float d1 = readDistanceCm(TRIG_1_PIN, ECHO_1_PIN);
  delay(SENSOR_SETTLE_DELAY_MS);
  float d2 = readDistanceCm(TRIG_2_PIN, ECHO_2_PIN);

  // DEBUG-AUSGABE JEDE SEKUNDE
  static unsigned long lastDebug = 0;
  if (millis() - lastDebug > 1000) {
    Serial.print("S1: ");
    Serial.print(d1);
    Serial.print(" cm | ");
    Serial.print("S2: ");
    Serial.print(d2);
    Serial.println(" cm");
    lastDebug = millis();
  }

  // Sensor 1 Logik
  if (d1 >= MIN_DISTANCE_CM && d1 <= MAX_DISTANCE_CM) {
    if (!sensor1Active && (millis() - lastEventMs > EVENT_COOLDOWN_MS)) {
      Serial.println(">>> EVENT: Sensor 1 Trigger!");
      mqttClient.publish("smartown/bridge/event", "BOAT_DETECTED_SENSOR_1");
      lastEventMs = millis();
      sensor1Active = true;
    }
  } else {
    sensor1Active = false;
  }

  // Sensor 2 Logik
  if (d2 >= MIN_DISTANCE_CM && d2 <= MAX_DISTANCE_CM) {
    if (!sensor2Active && (millis() - lastEventMs > EVENT_COOLDOWN_MS)) {
      Serial.println(">>> EVENT: Sensor 2 Trigger!");
      mqttClient.publish("smartown/bridge/event", "BOAT_DETECTED_SENSOR_2");
      lastEventMs = millis();
      sensor2Active = true;
    }
  } else {
    sensor2Active = false;
  }

  delay(LOOP_DELAY_MS);
}
