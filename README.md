<div align="center">

# SmarTown

Eine IoT-Demostadt auf Basis von ESP32, MQTT, Spring Boot und Vue mit Live-Steuerung über das Web, automatischer Klappbrücke, smarte Flugzeugerkennung mit dynamischer Rampenlichtanpassung 
sowie intelligenter Stadtbeleuchtung gesteuert nach Uhrzeit oder Umgebungshelligkeit.

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

[Demo](#demo) • [Überblick](#überblick) • [Einrichtung](#einrichtung)

</div>

## Demo

<div align="center">
  <p><strong>Live-Demo</strong></p>
  <video src="https://github.com/user-attachments/assets/cc608daf-67d7-4f6e-a3fe-bfc2622e0c72" controls muted playsinline width="900">
  </video>
</div>

<div align="center">
  <p><strong>Dashboard</strong></p>
  <img src="img/Dashboard.png" alt="Dashboard-Übersicht der SmarTown-Weboberfläche" width="100%" />
</div>

<div align="center">
  <p><strong>Lux-Chart</strong></p>
  <img src="img/LuxChart.png" alt="Lux-Verlauf im SmarTown-Dashboard" width="100%" />
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

Aktuelle Modulaufteilung:
- ESP32 1: Laternen in der Stadt
- ESP32 2: Flughafen mit Laternen und Ultraschallwellensensor
- ESP32 3: klappbare Bruecke
