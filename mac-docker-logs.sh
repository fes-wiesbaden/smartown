#!/bin/bash

# Array mit den vier Container-Namen
CONTAINERS=(
  "iot-smartown-gruppe-1-mqtt-1"
  "iot-smartown-gruppe-1-mariadb-1"
  "iot-smartown-gruppe-1-backend-1"
  "iot-smartown-gruppe-1-frontend-1"
)

echo "Öffne 4 neue Terminal-Fenster..."

# Schleife durch jeden Container
for container in "${CONTAINERS[@]}"; do
  # osascript ist der Mac-spezifische Befehl, um AppleScript auszuführen
  osascript -e "tell application \"Terminal\" to do script \"docker logs --follow $container\""
done

echo "Fertig!"
