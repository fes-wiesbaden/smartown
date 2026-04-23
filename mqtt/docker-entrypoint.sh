#!/bin/sh
set -eu

: "${MQTT_USERNAME:?MQTT_USERNAME is required}"
: "${MQTT_PASSWORD:?MQTT_PASSWORD is required}"

if [ "$MQTT_PASSWORD" = "change-me" ] || [ "$MQTT_PASSWORD" = "replace-with-strong-password" ]; then
  echo "MQTT_PASSWORD must be changed before starting the broker." >&2
  exit 1
fi

mosquitto_passwd -b -c /mosquitto/config/password_file "$MQTT_USERNAME" "$MQTT_PASSWORD"

exec mosquitto -c /mosquitto/config/mosquitto.conf
