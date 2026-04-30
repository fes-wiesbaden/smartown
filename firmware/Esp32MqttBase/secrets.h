// Local development secrets for Arduino CLI.
// Replace these placeholders with your real values.

#pragma once

constexpr char WIFI_SSID[] = "FES-SuS";
constexpr char WIFI_PASSWORD[] = "SuS-WLAN!Key24";

constexpr char MQTT_HOST[] = "10.93.135.232";
constexpr uint16_t MQTT_PORT = 1883;
constexpr char MQTT_USERNAME[] = "smartown";
constexpr char MQTT_PASSWORD[] = "mqttPw";

constexpr char MQTT_CLIENT_ID[] = "esp32-xx5r69";
constexpr char MQTT_TOPIC_STATE[] = "smartown/esp32/xx5r69/state";
constexpr char MQTT_TOPIC_COMMAND[] = "smartown/esp32/xx5r69/cmd";
