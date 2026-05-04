# Architektur SmarTown

[Ziel](#ziel) | [Systemkontext](#systemkontext) | [Technologiestack](#technologiestack) | [Laufzeitarchitektur](#laufzeitarchitektur) | [Verantwortlichkeiten](#verantwortlichkeiten) | [Fachliche Leitplanken](#fachliche-leitplanken) | [Kommunikationsmuster](#kommunikationsmuster) | [Datenfluss](#datenfluss) | [Entwicklungsworkflow](#entwicklungsworkflow) | [Offene Architekturentscheidungen](#offene-architekturentscheidungen)

## Ziel
Dieses Dokument konkretisiert die aktuell getroffenen Architekturentscheidungen fuer das Projekt SmarTown. Es beschreibt die Systemgrenzen, den technischen Aufbau, den ersten fachlichen Fokus und offene Punkte.

## Systemkontext
Das System steuert und ueberwacht ein physisches Smart-City-Demomodell. Sensorwerte kommen von drei ESP32-Modulen. Fachentscheidungen werden im Backend getroffen. Das Frontend visualisiert Live-Daten und erlaubt manuelle Eingriffe. Die Kommunikation zwischen zentralen Komponenten laeuft ueber MQTT, REST und WebSocket.

## Technologiestack
- Firmware: Arduino auf ESP32
- Backend: Java 21, Spring Boot 3.5.x
- Frontend: Vue 3, Vite, TypeScript, Tailwind CSS
- Messaging: MQTT 3.1.1
- Datenbank: MariaDB
- Deployment: Docker auf Raspberry Pi

## Laufzeitarchitektur
Im Finalbetrieb laufen vier Container auf dem Raspberry Pi:

1. MQTT-Broker
2. MariaDB
3. Backend
4. Frontend

Frontend und Backend werden getrennt deployt. Das Frontend laeuft als statischer Vue-Build in Nginx und leitet Backend-Routen im Docker-Netzwerk an den Backend-Container weiter.

## Verantwortlichkeiten
### ESP32
- Liest Sensorwerte ein
- Verbindet Sensorik und Aktorik mit dem zentralen System
- Empfaengt MQTT-Commands
- Setzt Aktoren physisch um
- Sendet State- und Event-Nachrichten

Aktuelle Projektentscheidung:
- ESP32 1: Laternen in der Stadt
- ESP32 2: Flughafen mit Laternen und Ultraschallwellensensor
- ESP32 3: klappbare Bruecke

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
- Aktuell ist `50 lx` als fester Startwert im ESP32-Sketch hinterlegt
- Laternen sind auch manuell ueber das Frontend schaltbar
- Ein manueller Override bleibt aktiv, bis er bewusst wieder aufgehoben wird
- Nach einem Neustart geht das System fuer Laternen wieder in den Modus `AUTO`
- Nach einem MQTT-Reconnect geht das System fuer Laternen ebenfalls wieder in den Modus `AUTO`

### Entscheidungsort
Die Laternen bilden aktuell eine bewusst lokale Ausnahme. Der ESP32 entscheidet im Modus `AUTO` direkt anhand des BH1750 ueber `AN` oder `AUS`, damit Sensorik und Aktorik auch bei Backend- oder Netzproblemen stabil zusammenbleiben. Backend und Frontend wechseln nur den Modus, visualisieren den Zustand und verteilen Live-Updates per WebSocket.

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
MQTT ist die Verbindung zwischen Backend und den drei ESP32-Modulen.

Das erste konkrete MQTT-Schema fuer das Modul `Laternen` ist in [mqtt/laternen.md](mqtt/laternen.md) dokumentiert.

Weitere MQTT-Dokumente:
- [mqtt/bruecke.md](mqtt/bruecke.md)
- [mqtt/flughafen.md](mqtt/flughafen.md)

Kurz:
- `command`: Backend -> passender ESP32
- `state`: ESP32 -> Backend
- `event`: fachliche Ereignisse fuer Nachvollziehbarkeit

Die Zuordnung erfolgt ueber modulbezogene Topics, nicht ueber direkte IP-Kommunikation vom Backend zum ESP32.
Beispiele:
- `smartown/lanterns/command`
- `smartown/airport/command`
- `smartown/bridge/command`

## Datenfluss
1. Ein Sensor liefert Messwerte an den zustaendigen ESP32.
2. Der ESP32 sendet die Rohdaten oder den aktuellen Zustand per MQTT an das Backend.
3. Das Backend bewertet die Daten anhand der Fachregeln.
4. Falls noetig sendet das Backend ein Command per MQTT an das passende Modul-Topic.
5. Der zustaendige ESP32 setzt den Aktorzustand um.
6. Der ESP32 meldet den neuen State zurueck.
7. Das Backend aktualisiert Persistenz und Live-Status.
8. Das Frontend erhaelt den neuen Zustand per WebSocket.

## Entwicklungsworkflow
1. Hardware und Firmware werden lokal per USB an den jeweiligen ESP32 geflasht und getestet.
2. Das Projekt setzt auf drei ESP32 mit getrennter Modulverantwortung.
3. Backend und Frontend werden zuerst mit Mock-Daten oder Simulatoren entwickelt.
4. Danach folgt die schrittweise Integration mit MQTT, REST, WebSocket und echter Hardware.
5. Im Finalbetrieb bleiben die drei ESP32 als getrennte Module erhalten.

## Offene Architekturentscheidungen
- Konkreter numerischer Schwellwert fuer den BH1750
- Anzahl der Laternen oder Zonen
- Umfang der Persistenz fuer Historien
