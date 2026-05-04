#include <HCSR04.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include "../../brueckensteuerung/secrets.h"

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
UltraSonicDistanceSensor distanceSensor(5, 4);
int pins[] = {21, 19, 32, 33, 25, 26, 27, 14, 12, 13};
bool run = true;
const char* MQTT_TOPIC_COMMAND = "airport";

void setup() {
  Serial.begin(9600);
  for (int i = 0; i < 10; i++) {
    pinMode(pins[i], OUTPUT);
  }
  /*ensureWifiConnected();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommand);
  ensureMqttConnected();*/
}

void loop() {
  while(run) {
    digitalWrite(pins[0], LOW);
    for (int i = 9; i > 0; i--) {
      if (2 < distanceSensor.measureDistanceCm() && distanceSensor.measureDistanceCm() < 40) {
        for (int i = 0; i < 10; i++) {
          digitalWrite(pins[i], LOW);
        }
        i = 0;
        flugzeugKommt();
      }
      digitalWrite(pins[i], HIGH);
      Serial.println(String("Pin: ") + pins[i]);
      delay(100);
      if (i == 9) {
        digitalWrite(pins[1], LOW);
      }
      else {
        digitalWrite(pins[i + 1], LOW);
      }
    }
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

  if (message.indexOf("ON") != -1) {
    Serial.println("Lichter an");
    run = true;
  } else if (message.indexOf("OFF") != -1) {
    Serial.println("Lichter aus");
    run = false;
  }
}

void flugzeugKommt() {
  while (true) {
    int distance = round(distanceSensor.measureDistanceCm());
    int stelle = (distance - 4) / 4;

    if(distance <= 0 || distance > 40) {
      break;
    }

    for(int i = 0; i < 10; i++) {
      digitalWrite(pins[i], LOW);
    }

    if (distance < 6) {
      Serial.println(String("Distanz: ") + distance);
      digitalWrite(pins[0], HIGH);
    }
    else {
      if(stelle < 0) stelle = 0;
      if(stelle > 9) stelle = 9;

      for(int i = stelle; i <= stelle + 2 && i < 10; i++) {
        digitalWrite(pins[i], HIGH);
      }
    }
    delay(200);
  }
}







