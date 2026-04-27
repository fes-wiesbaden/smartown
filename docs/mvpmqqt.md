# MVP MQTT Workflow

## Ziel
Dieses Dokument beschreibt den kleinsten durchgehenden MQTT-MVP fuer die Laternen:

1. ESP32 verbindet sich per Wi-Fi mit dem Schulnetz.
2. ESP32 sendet `state`- und `event`-Payloads an den MQTT-Broker auf dem Laptop.
3. Backend liest die MQTT-Nachrichten, stellt den aktuellen Zustand per REST bereit und pusht Updates per WebSocket an das Frontend.
4. Frontend zeigt den Zustand an und sendet Moduswechsel per REST an das Backend.
5. Backend publiziert daraus `command`-Nachrichten zurueck an den ESP32.

## Wi-Fi und IPs
- Test-MVP Broker auf dem Laptop: `10.93.135.232:1883`
- Raspberry Pi fuer spaetere Zielumgebung: `10.93.128.204`
- Reserve fuer ESP32: `10.93.128.205` bis `10.93.128.208`
- Subnetzmaske: `255.255.240.0`
- Gateway: `10.93.128.1`
- DNS: `10.93.128.1`

Der ESP32 braucht eine funktionierende Wi-Fi-Verbindung ins Schulnetz. Fuer diesen Test-MVP verbindet sich der ESP32 zum MQTT-Broker auf der aktuellen Laptop-IP `10.93.135.232`. Das ist bewusst nicht produktionsreif. Der Raspberry bleibt die spaetere Zieladresse.

## Firmware-MVP
Neuer Sketch:

```text
firmware/laternen/laternen_mqtt_mvp/
```

Lokale Vorbereitung:

1. `secrets.example.h` nach `secrets.h` kopieren.
2. Wi-Fi- und MQTT-Werte lokal eintragen.
3. Sketch bauen und per USB auf den ESP32 flashen.

Beispiel fuer Build und Upload:

```bash
arduino-cli compile firmware/laternen/laternen_mqtt_mvp
arduino-cli upload -p /dev/ttyUSB0 --fqbn esp32:esp32:esp32 firmware/laternen/laternen_mqtt_mvp
```

Der Upload laeuft lokal per USB. Die spaetere Kommunikation zwischen ESP32 und MQTT-Broker laeuft danach ueber Wi-Fi.

Arduino-IDE-Workflow:

1. Ordner `firmware/laternen/laternen_mqtt_mvp/` in der Arduino IDE oeffnen.
2. Falls noetig `secrets.example.h` lokal zu `secrets.h` kopieren und die Wi-Fi- und MQTT-Werte eintragen.
3. Board `ESP32 Dev Module` oder das passende ESP32-Board waehlen.
4. Den aktuell richtigen USB-Port des ESP32 in der IDE auswaehlen.
5. Sketch pruefen und danach per Upload auf das Board flashen.

Die IDE braucht den USB-Port nur fuer Build, Upload und seriellen Monitor. Die MQTT-Kommunikation des laufenden ESP32 mit dem Broker laeuft anschliessend ueber Wi-Fi.

Der Sketch:
- liest BH1750-Luxwerte
- steuert die Laternen ueber den PCA9685
- verbindet sich per Wi-Fi und MQTT
- sendet JSON auf `smartown/lanterns/state`
- sendet JSON auf `smartown/lanterns/event`
- empfaengt JSON auf `smartown/lanterns/command`

## Mehrere Logiken auf dem ESP32
Ein ESP32 kann nicht mehrere getrennte Arduino-Sketches gleichzeitig ausfuehren. Auf dem Board laeuft immer genau ein geflashtes Firmware-Image.

Aktuelle Projektentscheidung:

- ESP32 1: Laternen in der Stadt
- ESP32 2: Flughafen mit Laternen und Ultraschallwellensensor
- ESP32 3: klappbare Bruecke

Fuer das Projekt bedeutet das:

- `firmware/laternen/laternen_mqtt_mvp/` ist aktuell die eigene Firmware fuer das Laternen-MVP.
- `firmware/brueckensteuerung/`, `firmware/laternen/helligkeitssteuerung_test/` und `firmware/Esp32MqttBase/` sind getrennte Sketche bzw. Test- oder Basisstaende.
- Diese Sketche laufen nicht parallel auf demselben ESP32.

Die beschlossene Architektur ist:

- ein ESP32 pro Fachmodul
- eine eigene Firmware pro Modul
- klare MQTT-Topics pro Modul

Beispiel fuer die Topic-Struktur:

- `smartown/lanterns/...`
- `smartown/airport/...`
- `smartown/bridge/...`

Der aktuelle MVP setzt davon zuerst das Modul `Laternen` um. Die anderen Module werden nach demselben Muster angebunden.

## MQTT Topics
- `smartown/lanterns/command`
- `smartown/lanterns/state`
- `smartown/lanterns/event`

## Payloads
Command vom Backend an den ESP32:

```json
{
  "action": "SET_MODE",
  "mode": "FORCED_ON"
}
```

State vom ESP32 an Backend und Frontend:

```json
{
  "mode": "AUTO",
  "lightState": "ON",
  "lux": 12.4,
  "online": true,
  "thresholdLux": 50.0
}
```

Event vom ESP32 an Backend und Frontend:

```json
{
  "type": "LIGHT_STATE_CHANGED",
  "lightState": "ON",
  "reason": "LOW_LUX"
}
```

## Backend-Schnittstellen
- REST Snapshot: `GET /api/lanterns`
- REST Moduswechsel: `PUT /api/lanterns/mode`
- WebSocket Live-Updates: `/ws/lanterns`

Beispiel fuer den REST-Command:

```bash
curl -X PUT http://localhost:8080/api/lanterns/mode \
  -H 'Content-Type: application/json' \
  -d '{"mode":"FORCED_OFF"}'
```

## Frontend-MVP
Das Frontend:
- laedt beim Start den Snapshot per REST
- verbindet sich danach mit dem WebSocket
- zeigt `mode`, `lightState`, `lux`, `thresholdLux`, `online` und das letzte Event an
- bietet drei Modi: `AUTO`, `FORCED_ON`, `FORCED_OFF`

## Testablauf
1. Docker-Stack mit MQTT, DB, Backend und Frontend starten.
2. ESP32-Sketch flashen.
3. Frontend unter `http://localhost:8081` oder lokal unter `http://localhost:5173` oeffnen.
4. Luxwert aendern oder Modus im Frontend umschalten.
5. Backend-REST, WebSocket und MQTT-Topics pruefen.

State live pruefen:

```bash
mosquitto_sub -h 10.93.135.232 -p 1883 -u smartown -P <MQTT_PASSWORT> -t smartown/lanterns/state -v
```

Events live pruefen:

```bash
mosquitto_sub -h 10.93.135.232 -p 1883 -u smartown -P <MQTT_PASSWORT> -t smartown/lanterns/event -v
```

Command manuell senden:

```bash
mosquitto_pub -h 10.93.135.232 -p 1883 -u smartown -P <MQTT_PASSWORT> \
  -t smartown/lanterns/command \
  -m '{"action":"SET_MODE","mode":"FORCED_ON"}'
```

## TODO
- Echten End-to-End-Test beweisen: Frontend -> Backend -> MQTT -> ESP32 -> MQTT -> Backend -> Frontend mit geflashter Hardware und Live-Logs.
