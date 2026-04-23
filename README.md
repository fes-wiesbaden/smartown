<div align="center">

# SmarTown

IoT-Demostadt mit ESP32, MQTT, Spring Boot, Vue und Live-Steuerung ueber das Web.

<sub><strong>Tech Stack</strong></sub>

[![Frontend: Vue 3, TypeScript 5, Vite 7](https://img.shields.io/badge/Frontend-Vue%203%20%7C%20TypeScript%205%20%7C%20Vite%207-42B883?style=for-the-badge&logo=vuedotjs&logoColor=white)](#techstack)
[![UI: Tailwind CSS 4](https://img.shields.io/badge/UI-Tailwind%20CSS%204-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](#techstack)
[![Backend: Java 21, Spring Boot 3.5](https://img.shields.io/badge/Backend-Java%2021%20%7C%20Spring%20Boot%203.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](#techstack)
[![Messaging: MQTT 3.1.1, WebSocket](https://img.shields.io/badge/Messaging-MQTT%203.1.1%20%7C%20WebSocket-7A3EF0?style=for-the-badge&logo=eclipsemosquitto&logoColor=white)](#techstack)
[![Data: MariaDB 11.8, Docker](https://img.shields.io/badge/Data-MariaDB%2011.8%20%7C%20Docker-003545?style=for-the-badge&logo=mariadb&logoColor=white)](#techstack)
[![Firmware: Arduino, ESP32](https://img.shields.io/badge/Firmware-Arduino%20%7C%20ESP32-00979D?style=for-the-badge&logo=arduino&logoColor=white)](#techstack)

[![Total Commits](https://img.shields.io/github/commit-activity/t/fes-wiesbaden/iot-smartown-gruppe-1?style=flat-square)](https://github.com/fes-wiesbaden/iot-smartown-gruppe-1/commits)
[![Branches](https://img.shields.io/github/branches/fes-wiesbaden/iot-smartown-gruppe-1?style=flat-square)](https://github.com/fes-wiesbaden/iot-smartown-gruppe-1/branches)
[![Last Commit](https://img.shields.io/github/last-commit/fes-wiesbaden/iot-smartown-gruppe-1/main?style=flat-square)](https://github.com/fes-wiesbaden/iot-smartown-gruppe-1/commits/main)
[![Stars](https://img.shields.io/github/stars/fes-wiesbaden/iot-smartown-gruppe-1?style=flat-square)](https://github.com/fes-wiesbaden/iot-smartown-gruppe-1/stargazers)
[![Forks](https://img.shields.io/github/forks/fes-wiesbaden/iot-smartown-gruppe-1?style=flat-square)](https://github.com/fes-wiesbaden/iot-smartown-gruppe-1/network/members)

[Überblick](#überblick) • [Anforderungen](#anforderungen) • [Netzwerk](#netzwerk) • [Lokale Entwicklung](#lokale-entwicklung) • [Vorhandene Hardware](#vorhandene-hardware) • [Muss-Funktionen](#muss-funktionen) • [Meilensteine](#meilensteine) • [Architektur](#architektur-grob) • [Entwicklungsworkflow](#entwicklungsworkflow) • [Entwicklungsregeln Git](#entwicklungsregeln-git) • [Datenfluss](#datenfluss)

</div>

## Überblick
Miniatur-"Smarte Stadt" als IoT-Demomodell. Mehrere Bereiche sind mit Sensoren und Aktoren ausgestattet und werden über eine Weboberfläche überwacht und gesteuert. Ereignisse lösen automatisierte Abläufe aus, einige Funktionen sind zusätzlich manuell schaltbar. Module sind einzeln testbar und laufen am Ende als Gesamtsystem in einer Live-Demo. Das Projekt soll mit Scrum bearbeitet werden.

## Anforderungen

- Erfassung von Sensordaten über Mikrocontroller und Sensoren
- Nutzung eines MQTT-Brokers zur Datenübertragung
- Verarbeitung der Sensordaten mit einer eigenen oder angepassten Anwendung
- Speicherung der Daten in einer MariaDB-Datenbank
- Visualisierung und Interaktion im Browser

## Netzwerk
| Gerät | IP-Adresse | Subnetzmaske | Gateway | DNS |
|---|---|---|---|---|
| Raspberry Pi | 10.93.128.204 | 255.255.240.0 | 10.93.128.1 | 10.93.128.1 |

## Lokale Entwicklung

### Voraussetzungen
- Git
- Docker mit Docker Compose
- Java 21
- Node.js 20.19 oder 22.12+

Danach abmelden und neu anmelden. Die `docker`-Gruppe hat weitreichende Rechte, deshalb nur eigene Entwickler-Accounts hinzufügen.

### Projekt kopieren

```bash
git clone https://github.com/fes-wiesbaden/iot-smartown-gruppe-1.git
cd iot-smartown-gruppe-1
cp .env.example .env
```

Die Datei `.env` enthält lokale Zugangsdaten und wird nicht committet. Vor dem Docker-Start `MQTT_PASSWORD` setzen. Bei Port-Konflikten `MARIADB_PORT`, `MQTT_PORT`, `BACKEND_PORT` oder `FRONTEND_PORT` in `.env` ändern.

### Lokale Entwicklung starten

MariaDB läuft lokal per Docker:

```bash
docker compose up -d mariadb
```

Backend lokal starten:

```bash
cd backend
./mvnw spring-boot:run
```

Backend-Tests nutzen Testcontainers und brauchen Docker-Zugriff:

```bash
cd backend
./mvnw test
```

Frontend lokal starten:

```bash
cd frontend
npm ci
npm run dev
```

URLs bei lokaler Entwicklung:

| Dienst | URL |
|---|---|
| Frontend Dev-Server | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Backend Healthcheck | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MQTT Broker | localhost:1883 |
| MariaDB | localhost:3306 |

### Gesamte App per Docker starten

Für Demo oder finalen Betrieb laufen vier Container: MQTT-Broker, MariaDB, Backend und Frontend. Compose baut MQTT, Backend und Frontend selbst. MariaDB nutzt das offizielle Image. Healthchecks erzwingen die Startreihenfolge: MQTT-Broker, MariaDB, Backend, Frontend.

```bash
docker compose up --build
```

URLs bei Docker Compose:

| Dienst | URL |
|---|---|
| Frontend | http://localhost:8081 |
| Backend | http://localhost:8080 |
| Backend Healthcheck | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MQTT Broker | localhost:1883 |
| MariaDB | localhost:3306 |

Wenn Backend oder MariaDB lokal und per Docker gleichzeitig laufen sollen, `BACKEND_PORT` oder `MARIADB_PORT` in `.env` ändern.

### Datenbank zurücksetzen

Nur ausführen, wenn lokale Daten gelöscht werden dürfen:

```bash
docker compose down -v
```

## Vorhandene Hardware
- 3x DC 5V Stepper Motor 28BYJ-48 mit Treiberboard
- 2x HW-131 Breadboard Power Supply Motorsteuerung für die Motoren
- 1x PCA9685 16-Channel 12-bit PWM Driver für Massen-LED-Steuerung
- 3x HC-SR04
- 2x JQC-3FF-S-Z mit 3 Pins (S, +, -)
- 1x ESP-WROOM-32 ESP-32S Development Board (CP2102 + 30PIN + Type C)
- 2x KY-026 IR Fire/Flame Detection
- Jede Menge Widerstände aller Art
- Breadboards jeder Größe
- Standard LED-Sortiment 3mm mit Vorwiderständen von Quadrios, Artikelnr: QUAD 1801O003
- Jede Menge Jumper-Kabel
- 1x Raspberry Pi 5, 4GB RAM mit SATA SSD 500 GB
- 1x BH1750

## Muss-Funktionen

### Skilift
- Über das Frontend ein-/ausschaltbar
- Statusanzeige (oben / unten)
- Step Motor DC Motor, welcher durchgängig schnur zieht und Skilift herumschiebt.

### Brücke
- **Sensor 1 (vor der Brücke):** erkennt anfahrendes Boot → Brücke fährt hoch
- **Sensor 2 (nach der Brücke):** erkennt, dass das Boot durch ist → Brücke fährt runter
- Not-Aus / Manuell-Override
- Der Stepper hebt die Brücke einseitig an

### Flughafen
- Landelichter schalten bei "Landung" nacheinander (Sequenz)
- Landung wird mit Ultraschallsensor gemessen
- Nachvollziehbar im Frontend

### Laternen
- Automatisch abhängig von Helligkeit (dunkel = an, hell = aus)
- Zusätzlich manuell über das Frontend schaltbar
- (Schwellwert über das Frontend einstellbar, finaler Wert wird im Projektverlauf ermittelt)

## Meilensteine
1. **Anforderungsanalyse** – Muss-/Kann-Funktionen, Backlog & Grobkonzept (Sensorik/Aktorik, Datenfluss, UI)
2. **3D-Design & Mechanik-Prototyping** – Skilift, Brücke, Mautstation, Flughafenbereich, Laternen
3. **Elektronikaufbau & Firmware-Basis** – Sensoren einlesen, Aktoren ansteuern, erste Tests je Modul
4. **Backend** – Spring Boot API, MQTT-Anbindung, Datenmodell (Status, Events, Mautwerte), Logik
5. **Frontend-Dashboard** – Vue.js Dashboard, Live-Status per WebSocket, Steuerung per REST, Anzeige Mautpreise + Events
6. **Integration & Tests** – Alle Muss-Funktionen, End-to-End Tests, Fehlerbehebung, Demo-Szenarien
7. **Optional-Module** – Bombenwarnsystem, Ampel, Zugübergang, Windrad, Baustelle (wenn Zeit reicht)
8. **Abschluss** – Dokumentation, Abnahme, Präsentation & Live-Demo

## Architektur (grob)
| Schicht | Inhalt |
|---|---|
| Hardware | ESP32, Sensoren wie BH1750 und Ultraschall, Aktoren wie Stepper, Relais und LEDs |
| Firmware | Arduino-basierte ESP32-Firmware, liest Sensoren ein, empfängt Befehle per MQTT und setzt Aktoren um |
| Raspberry Pi | Zentrale Plattform für den Finalbetrieb mit Docker: MQTT-Broker, MariaDB, Spring Boot Backend und Vue-/Nginx-Frontend |
| Backend | REST-API für Steuerbefehle, MQTT-Subscriber/Publisher, Entscheidungslogik, Speicherung in MariaDB, Weitergabe von Live-Daten per WebSocket |
| Frontend | Vue-Dashboard für Live-Status, Schalter und Parametrierung wie Schwellwerte |
| Datenbank | MariaDB zur Speicherung von Zuständen, Konfiguration und optional später Historien |

## Entwicklungsworkflow
1. Während der Entwicklung können mehrere ESP32 parallel genutzt werden. Im finalen Produkt wird nur ein ESP32 eingesetzt.
2. Hardwaretests laufen direkt per USB-C/USB am Laptop. Sensorik, Aktorik, Flashen und serielle Logs werden lokal getestet. Solange nur die serielle Verbindung genutzt wird, spielen statische IP-Adressen keine Rolle.
3. Backend, Frontend und Datenbank werden zunächst lokal mit Mock-Daten oder einem kleinen Simulator für Sensorwerte entwickelt.
4. In der Integrationsphase werden Firmware, MQTT, REST, WebSocket und Hardware schrittweise zusammengeführt. Erst ab der Netzwerkintegration von ESP32 und Raspberry Pi sind statische IP-Adressen relevant.
5. Im Finalbetrieb läuft die Anwendung dann auf dem Raspberry Pi mit Docker: vier Container für MQTT-Broker, MariaDB, Backend und Frontend.

## Entwicklungsregeln Git
1. Es gibt die Branches `main` und `dev`.
2. Jeder arbeitet immer in einem eigenen Branch. Der Branchname beginnt immer mit dem eigenen Namen, z. B. `jan`.
3. Subbranches beginnen ebenfalls mit dem eigenen Namen und danach optional mit dem Feature, z. B. `jan-frontend` oder `jan-mqtt-fix`.
4. Es wird nicht direkt auf `main` gearbeitet.
5. Feature- und Personen-Branches werden zuerst in `dev` gemergt.
6. Erst wenn der Stand auf `dev` gemeinsam geprüft und vereinheitlicht wurde, wird von `dev` nach `main` gemergt.

## Datenfluss
1. Sensoren liefern Messwerte an den ESP32.
2. Der ESP32 sendet Zustände und Sensordaten per MQTT an den Raspberry Pi.
3. Das Spring Boot Backend verarbeitet die MQTT-Nachrichten, trifft die Fachentscheidungen und speichert relevante Daten in der MariaDB.
4. Das Backend überträgt Live-Daten per WebSocket an das Vue-Frontend.
5. Steuerbefehle und Konfigurationsänderungen aus dem Frontend gehen per REST an das Backend.
6. Das Backend sendet die resultierenden Steuerbefehle per MQTT an den ESP32.
7. Der ESP32 setzt die Aktion um und meldet den neuen Status erneut per MQTT zurück.
