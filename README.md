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

[Überblick](#überblick) • [Einrichtung](#einrichtung) • [Anforderungen](#anforderungen) • [Netzwerk](#netzwerk) • [Vorhandene Hardware](#vorhandene-hardware) • [Muss-Funktionen](#muss-funktionen) • [Meilensteine](#meilensteine) • [Architektur](#architektur-grob) • [Entwicklungsworkflow](#entwicklungsworkflow) • [Entwicklungsregeln Git](#entwicklungsregeln-git) • [Datenfluss](#datenfluss)

</div>

## Überblick
Miniatur-"Smarte Stadt" als IoT-Demomodell. Mehrere Bereiche sind mit Sensoren und Aktoren ausgestattet und werden über eine Weboberfläche überwacht und gesteuert. Ereignisse lösen automatisierte Abläufe aus, einige Funktionen sind zusätzlich manuell schaltbar. Module sind einzeln testbar und laufen am Ende als Gesamtsystem in einer Live-Demo. Das Projekt soll mit Scrum bearbeitet werden.

## Einrichtung

### Voraussetzungen

Einrichtung nur mit Docker:

- Git
- Docker mit Docker Compose

Für lokale Entwicklung ohne Backend-/Frontend-Container zusaetzlich:

- Java 21
- Node.js 20.19 oder 22.12+

### Projekt kopieren

```bash
git clone https://github.com/fes-wiesbaden/smartown.git
cd smartown
cp .env.example .env
```

### Lokale Entwicklung starten

MQTT-Broker und MariaDB laufen lokal per Docker:

```bash
docker compose up -d mqtt mariadb
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

**Alle Ports müssen frei sein!**

Ports: **1883; 3306; 8080; 8081;**
```bash
docker compose up --build -d
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

### Lokale Compose-Umgebung vollständig löschen

Nur ausführen, wenn alle lokalen Docker-Compose-Daten und Images dieses Projekts gelöscht werden dürfen.
```bash
docker compose down -v --rmi all 
```

## Netzwerk
Statische Adressen fuer das Projekt:

| Geraet | IP-Adresse | Hinweis |
|---|---|---|
| Raspberry Pi | 10.93.128.204 | Docker-Host, MQTT-Broker, Backend, Frontend, MariaDB |
| Reserve | 10.93.128.205 | frei fuer ESP32 oder weiteres Geraet |
| Reserve | 10.93.128.206 | frei fuer ESP32 oder weiteres Geraet |
| Reserve | 10.93.128.207 | frei fuer ESP32 oder weiteres Geraet |
| Reserve | 10.93.128.208 | frei fuer ESP32 oder weiteres Geraet |

Feste Netzparameter fuer alle statischen Geraete:

| Parameter | Wert |
|---|---|
| Subnetzmaske | 255.255.240.0 |
| Gateway | 10.93.128.1 |
| DNS | 10.93.128.1 |


## Architektur (grob)
| Schicht             | Inhalt                                                                                                                                     |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Hardware            | Drei ESP32, Sensoren wie BH1750 und Ultraschall, Aktoren wie Stepper und LEDs                                                              |
| Firmware            | Arduino-basierte ESP32-Firmware pro Modul, liest Sensoren ein, empfängt Befehle per MQTT und setzt Aktoren um                              |
| Raspberry Pi/Laptop | Zentrale Plattform für den Finalbetrieb mit Docker: MQTT-Broker, MariaDB, Spring Boot Backend und Vue-/Nginx-Frontend                      |
| Backend             | REST-API für Steuerbefehle, MQTT-Subscriber/Publisher, Entscheidungslogik, Speicherung in MariaDB, Weitergabe von Live-Daten per WebSocket |
| Frontend            | Vue-Dashboard für Live-Status, Schalter und Parametrierung wie Schwellwerte                                                                |
| Datenbank           | MariaDB zur Speicherung von Zuständen, Konfiguration und optional später Historien                                                         |

## Entwicklungsworkflow
1. Das Projekt nutzt drei ESP32 mit klarer Modultrennung. Ein Sketch laeuft immer genau auf einem ESP32.
2. Hardwaretests laufen direkt per USB-C/USB am Laptop. Sensorik, Aktorik, Flashen und serielle Logs werden lokal getestet. Solange nur die serielle Verbindung genutzt wird, spielen statische IP-Adressen keine Rolle.
3. Backend, Frontend und Datenbank werden zunächst lokal mit Mock-Daten oder einem kleinen Simulator für Sensorwerte entwickelt.
4. In der Integrationsphase werden Firmware, MQTT, REST, WebSocket und Hardware schrittweise zusammengeführt. Erst ab der Netzwerkintegration von ESP32 und Raspberry Pi sind statische IP-Adressen relevant.
5. Im Finalbetrieb läuft die Anwendung dann auf dem Raspberry Pi/Laptop mit Docker: vier Container für MQTT-Broker, MariaDB, Backend und Frontend.

Aktuelle Modulaufteilung:
- ESP32 1: Laternen in der Stadt
- ESP32 2: Flughafen mit Laternen und Ultraschallwellensensor
- ESP32 3: klappbare Bruecke

## Entwicklungsregeln Git
1. Es gibt die Branches `main` und `dev`.
2. Jeder arbeitet immer in einem eigenen Branch. Der Branchname beginnt immer mit dem eigenen Namen, z. B. `jan`.
3. Subbranches beginnen ebenfalls mit dem eigenen Namen und danach optional mit dem Feature, z. B. `jan-frontend` oder `jan-mqtt-fix`.
4. Es wird nicht direkt auf `main` gearbeitet.
5. Feature- und Personen-Branches werden zuerst in `dev` gemergt.
6. Erst wenn der Stand auf `dev` gemeinsam geprüft und vereinheitlicht wurde, wird von `dev` nach `main` gemergt.