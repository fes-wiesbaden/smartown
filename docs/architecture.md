# Architektur SmarTown

[Ziel](#ziel) | [Systemkontext](#systemkontext) | [Technologiestack](#technologiestack) | [Laufzeitarchitektur](#laufzeitarchitektur) | [Verantwortlichkeiten](#verantwortlichkeiten) | [Fachliche Leitplanken](#fachliche-leitplanken) | [Kommunikationsmuster](#kommunikationsmuster) | [Datenfluss](#datenfluss) | [Entwicklungsworkflow](#entwicklungsworkflow) | [Offene Architekturentscheidungen](#offene-architekturentscheidungen)

## Ziel
Dieses Dokument konkretisiert die aktuell getroffenen Architekturentscheidungen fuer das Projekt SmarTown. Es beschreibt die Systemgrenzen, den technischen Aufbau, den ersten fachlichen Fokus und offene Punkte.

## Systemkontext
Das System steuert und ueberwacht ein physisches Smart-City-Demomodell. Sensorwerte kommen von einem ESP32. Fachentscheidungen werden im Backend getroffen. Das Frontend visualisiert Live-Daten und erlaubt manuelle Eingriffe. Die Kommunikation zwischen zentralen Komponenten laeuft ueber MQTT, REST und WebSocket.

## Technologiestack
- Firmware: Arduino auf ESP32
- Backend: Java 21, Spring Boot 3.5.x
- Frontend: Vue 3, Vite, TypeScript, Tailwind CSS
- Messaging: MQTT 3.1.1
- Datenbank: MariaDB
- Deployment: Docker auf Raspberry Pi

## Laufzeitarchitektur
Im Finalbetrieb laufen drei Container auf dem Raspberry Pi:

1. MQTT-Broker
2. Anwendung mit Backend und Frontend
3. MariaDB

Frontend und Backend werden gemeinsam als eine Anwendung deployt. Damit bleibt das geplante Deployment bei drei Containern.

## Verantwortlichkeiten
### ESP32
- Liest Sensorwerte ein
- Verbindet Sensorik und Aktorik mit dem zentralen System
- Empfaengt MQTT-Commands
- Setzt Aktoren physisch um
- Sendet State- und Event-Nachrichten

### Backend
- Zentrale Fachlogik
- Bewertet Sensordaten und trifft Entscheidungen
- Verwaltet Konfiguration wie Schwellwerte
- Stellt REST-Endpunkte fuer Bedienung und Konfiguration bereit
- Sendet Live-Status per WebSocket an das Frontend
- Persistiert relevante Daten in MariaDB
- Publiziert Commands an den ESP32

### Frontend
- Visualisiert Live-Status
- Bietet manuelle Steuerung
- Erlaubt Konfigurationsaenderungen wie Schwellwerte
- Zeigt aktuelle Betriebsmodi und letzte bekannte Zustaende

## Fachliche Leitplanken
### Laternen

Aktuell bekannte Regeln:
- Automatikbetrieb basiert auf Helligkeitswerten des einzigen BH1750
- Die konkrete Schwellwert-Zahl ist noch nicht festgelegt und wird im Projekt ermittelt
- Laternen sind auch manuell ueber das Frontend schaltbar
- Ein manueller Override bleibt aktiv, bis er bewusst wieder aufgehoben wird
- Nach einem Neustart geht das System fuer Laternen wieder in den Modus `AUTO`

### Entscheidungsort
Die Fachentscheidung liegt im Backend. Der ESP32 entscheidet nicht selbst ueber Anwendungslogik, sondern liefert Messwerte und setzt Befehle um.

## Kommunikationsmuster
### REST
REST dient fuer Benutzeraktionen und Konfiguration.

Beispiele:
- Schalter im Frontend
- Aenderung von Schwellwerten
- Umschalten zwischen `AUTO` und manuellem Override

### WebSocket
WebSocket dient fuer Live-Status vom Backend zum Frontend.

Ziel:
- Dashboard ohne Polling
- Sofort sichtbare Statusaenderungen

### MQTT
MQTT ist die Verbindung zwischen Backend und ESP32.

Das erste konkrete MQTT-Schema fuer das Modul `Laternen` ist in [mqtt/laternen.md](mqtt/laternen.md) dokumentiert.

Weitere MQTT-Dokumente:
- [mqtt/bruecke.md](mqtt/bruecke.md)
- [mqtt/skilift.md](mqtt/skilift.md)
- [mqtt/flughafen.md](mqtt/flughafen.md)

Kurz:
- `command`: Backend -> ESP32
- `state`: ESP32 -> Backend
- `event`: fachliche Ereignisse fuer Nachvollziehbarkeit

## Datenfluss
1. Ein Sensor liefert Messwerte an den ESP32.
2. Der ESP32 sendet die Rohdaten oder den aktuellen Zustand per MQTT an das Backend.
3. Das Backend bewertet die Daten anhand der Fachregeln.
4. Falls noetig sendet das Backend ein Command per MQTT an den ESP32.
5. Der ESP32 setzt den Aktorzustand um.
6. Der ESP32 meldet den neuen State zurueck.
7. Das Backend aktualisiert Persistenz und Live-Status.
8. Das Frontend erhaelt den neuen Zustand per WebSocket.

## Entwicklungsworkflow
1. Hardware und Firmware werden lokal mit direkt angeschlossenem ESP32 getestet.
2. Waehrend der Entwicklung koennen mehrere ESP32 parallel verwendet werden.
3. Backend und Frontend werden zuerst mit Mock-Daten oder Simulatoren entwickelt.
4. Danach folgt die schrittweise Integration mit MQTT, REST, WebSocket und echter Hardware.
5. Im finalen Produkt wird nur ein ESP32 verwendet.

## Offene Architekturentscheidungen
- Konkreter numerischer Schwellwert fuer den BH1750
- Anzahl der Laternen oder Zonen
- Umfang der Persistenz fuer Historien