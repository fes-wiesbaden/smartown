# MQTT Setup Workflow

[Ziel](#ziel) | [Voraussetzungen](#voraussetzungen) | [Ordnerstruktur](#ordnerstruktur) | [Verbindliche Regeln](#verbindliche-regeln) | [sketchyaml](#sketchyaml) | [secretsh](#secretsh) | [Workflow](#workflow) | [MQTT-Test](#mqtt-test) | [Hinweise](#hinweise)

## Ziel
Dieses Dokument beschreibt den verbindlichen Entwicklungsworkflow fuer ESP32-Module, die per MQTT an SmarTown angebunden werden. Ziel ist ein reproduzierbares Setup fuer Build, Upload und MQTT-Integration mit Arduino CLI und Arduino IDE.

## Voraussetzungen
- installierte Arduino CLI oder Arduino IDE
- MQTT-Bibliotheken pro Sketch ueber `sketch.yaml`
- ein Sketch-Ordner im Projekt, empfohlen unter `firmware/`
- ein lauffaehiger MQTT-Broker
- ein ESP32 mit bekanntem Port, aktuell meist `/dev/ttyUSB0`

## Ordnerstruktur
Jedes Modul bekommt einen eigenen Sketch-Ordner.

Beispiel:
```text
iot-smartown-gruppe-1/
└── firmware/
    └── Esp32MqttBase/
        ├── secrets.h
        ├── sketch.yaml
        └── Esp32MqttBase.ino
```

Regeln:
- ein Sketch = ein Ordner
- die Hauptdatei `.ino` muss gleich heissen wie der Ordner
- jedes MQTT-Modul braucht ein eigenes `sketch.yaml`
- sensible Daten bleiben in `secrets.h`
- der Sketch muss in Arduino IDE oeffenbar und in Arduino CLI buildbar sein

## Verbindliche Regeln
- Commands kommen vom Backend, State und Event kommen vom ESP32
- Payloads fuer MQTT werden als JSON gesendet
- neue Libraries werden nicht stillschweigend global vorausgesetzt, sondern im `sketch.yaml` des Sketches eingetragen
- reale Zugangsdaten gehoeren nur in lokale `secrets.h` Dateien
- Fachlogik bleibt im Backend, der ESP32 setzt Commands um und meldet Zustaende
- ein Sketch entspricht genau einem ESP32-Modul
- das Projekt nutzt aktuell drei ESP32-Module: Stadtlaternen, Flughafen und klappbare Bruecke

## sketch.yaml
`sketch.yaml` ist Pflicht. Arduino IDE ignoriert die Datei, Arduino CLI nutzt sie fuer reproduzierbare Builds. Darin stehen mindestens:
- Board
- benoetigte Plattformen
- benoetigte Libraries

Port und Protokoll koennen lokal gesetzt werden. Sie muessen nicht fest im Repo stehen.

Beispiel:
```yaml
default_fqbn: esp32:esp32:esp32
default_profile: dev

profiles:
  dev:
    fqbn: esp32:esp32:esp32
    platforms:
      - platform: esp32:esp32 (3.3.8)
    libraries:
      - ArduinoJson (7.4.3)
      - PubSubClient (2.8.0)
```

## secrets.h
`secrets.h` enthaelt nur lokale Zugangsdaten und Adressen.

Beispiel:
```cpp
#pragma once

constexpr char WIFI_SSID[] = "WLAN_NAME";
constexpr char WIFI_PASSWORD[] = "WLAN_PASSWORT";

# mqqt host ip (z. B. Laptop)
constexpr char MQTT_HOST[] = "10.93.135.232";
constexpr uint16_t MQTT_PORT = 1883;
constexpr char MQTT_USERNAME[] = "smartown";
constexpr char MQTT_PASSWORD[] = "mqttPw";

constexpr char MQTT_CLIENT_ID[] = "esp32-lantern-1";
```

## Workflow
1. MQTT-Schema in `docs/mqtt/<modul>.md` festlegen.
2. Sketch-Ordner im Projekt unter `firmware/...` anlegen.
3. `sketch.yaml` anlegen.
4. `secrets.h` lokal befuellen.
5. Sketch implementieren.
6. Sketch in Arduino IDE oeffnen und pruefen, ob Ordnername und `.ino`-Dateiname zusammenpassen.
7. In Arduino IDE:
- passendes Board waehlen
- passenden Port waehlen
- benoetigte Libraries installieren, falls die IDE sie noch nicht kennt
8. Fuer Arduino CLI Board und Port setzen:
```bash
arduino-cli board attach -p /dev/ttyUSB0 -b esp32:esp32:esp32 <SKETCH_ORDNER>
```
9. Build pruefen:
```bash
arduino-cli compile <SKETCH_ORDNER>
```
10. Optional in Arduino IDE einmal verifizieren oder hochladen.
11. Per CLI auf ESP32 laden:
```bash
arduino-cli upload -p /dev/ttyUSB0 --fqbn esp32:esp32:esp32 <SKETCH_ORDNER>
```
12. MQTT-Nachrichten gegen Broker pruefen.

Aktuelle Projektaufteilung:
- `Laternen`: eigener ESP32 fuer die Stadtlaternen
- `Flughafen`: eigener ESP32 fuer Flughafenlaternen und Ultraschallwellensensor
- `Bruecke`: eigener ESP32 fuer die klappbare Bruecke

Das Backend adressiert diese Module spaeter ueber eigene MQTT-Topic-Bereiche wie `smartown/lanterns/...`, `smartown/airport/...` und `smartown/bridge/...`.

## MQTT-Test
State abonnieren:
```bash
mosquitto_sub -h 10.93.135.232 -p 1883 -u smartown -P mqttPw -t smartown/lanterns/state
```

Events abonnieren:
```bash
mosquitto_sub -h 10.93.135.232 -p 1883 -u smartown -P mqttPw -t smartown/lanterns/event
```

Command senden:
```bash
mosquitto_pub -h 10.93.135.232 -p 1883 -u smartown -P mqttPw -t smartown/lanterns/command -m '{"action":"SET_MODE","mode":"ON"}'
```

## Hinweise
- Wenn `arduino-cli board list` das Board als `Unknown` zeigt, kann Compile mit explizitem `--fqbn` trotzdem funktionieren.
- Arduino IDE und Arduino CLI muessen dasselbe Boardziel verwenden, sonst entstehen schwer nachvollziehbare Unterschiede beim Build.
- Fuer echte Automatik muss der jeweilige Sensor im Sketch angeschlossen und implementiert sein.
- Retained `state`-Nachrichten helfen dem Backend, den letzten bekannten Zustand sofort zu sehen.
- `event`-Nachrichten sind fuer nachvollziehbare Zustandswechsel gedacht, nicht als Ersatz fuer `state`.
