# SmarTown

[Überblick](#überblick) | [Anforderungen](#anforderungen) | [Zeitraum](#zeitraum) | [Projektumfeld](#projektumfeld) | [Netzwerk](#netzwerk) | [Techstack](#techstack) | [Vorhandene Hardware](#vorhandene-hardware) | [Muss-Funktionen](#muss-funktionen) | [Kann-Funktionen](#kann-funktionen-optional-wenn-zeit-reicht) | [Meilensteine](#meilensteine) | [Architektur](#architektur-grob) | [Entwicklungsworkflow](#entwicklungsworkflow) | [Entwicklungsregeln Git](#entwicklungsregeln-git) | [Datenfluss](#datenfluss)

## Überblick
Miniatur-"Smarte Stadt" als IoT-Demomodell. Mehrere Bereiche sind mit Sensoren und Aktoren ausgestattet und werden über eine Weboberfläche überwacht und gesteuert. Ereignisse lösen automatisierte Abläufe aus, einige Funktionen sind zusätzlich manuell schaltbar. Module sind einzeln testbar und laufen am Ende als Gesamtsystem in einer Live-Demo. Das Projekt soll mit Scrum bearbeitet werden.

## Anforderungen

- Erfassung von Sensordaten über Mikrocontroller und Sensoren
- Nutzung eines MQTT-Brokers zur Datenübertragung
- Verarbeitung der Sensordaten mit einer eigenen oder angepassten Anwendung
- Speicherung der Daten in einer MySQL- oder SQLite-Datenbank
- Visualisierung und Interaktion im Browser

## Zeitraum
| Start | Ende |
|---|---|
| 22.01.2026 | 06.05.2026 |

## Netzwerk
| Gerät | IP-Adresse | Subnetzmaske | Gateway | DNS |
|---|---|---|---|---|
| Raspberry Pi | 10.93.128.204 | 255.255.240.0 | 10.93.128.1 | 10.93.128.1 |

## Techstack
- Java
- Spring Boot
- Vue.js
- TypeScript
- Docker
- MQTT
- MySQL oder SQLite
- WebSocket
- REST-API

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
- 1x BY1750

## Muss-Funktionen

### Skilift
- Über das Frontend ein-/ausschaltbar
- Statusanzeige (oben / unten)
- Step Motor DC Motor, welcher durchgängig schnur zieht und Skilift herumschiebt.

### Brücke
- **Sensor 1 (vor der Brücke):** erkennt anfahrendes Boot → Brücke fährt hoch
- **Sensor 2 (nach der Brücke):** erkennt, dass das Boot durch ist → Brücke fährt runter
- Not-Aus / Manuell-Override

### Flughafen
- Landelichter schalten bei "Landung" nacheinander (Sequenz)
- Landung wird mit Ultraschallsensor gemessen
- Nachvollziehbar im Frontend

### Laternen
- Automatisch abhängig von Helligkeit (dunkel = an, hell = aus)
- Schwellwert optional einstellbar

## Kann-Funktionen (optional, wenn Zeit reicht)
- **Mautstation:** Gewicht erfassen, Preis nach Gewicht berechnen, Anzeige im Frontend (optional Speicherung als Verlauf)
- **Bombenwarnsystem (Simulation):** Warn-Event schaltet rote Warnlichter (optional akustisches Signal), Ereignis im Frontend sichtbar/logbar
- **Ampelsystem**
- **Zugübergang** mit Schranken
- **Windräder** als "Energie-Event" für Laternen
- **Baustellenbezirk** mit Zutrittswarnung

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
| Hardware | ESP32, Sensoren (Helligkeit, Gewicht), Aktoren (Servo, Motor, LEDs) |
| Firmware | Sensoren einlesen, Aktoren steuern, MQTT-Kommunikation |
| Raspberry Pi | Zentrale Plattform für den Finalbetrieb mit Docker, dabei 2 Container: 1 Container für den MQTT-Broker und 1 Container für die Anwendung mit Spring Boot Backend, Vue.js Frontend und Datenbank |
| Backend | REST-API für Steuerbefehle, MQTT-Subscriber/Publisher, Event-Logik, Speicherung in DB, Weitergabe von Live-Daten per WebSocket |
| Frontend | Vue.js Dashboard für Live-Status, Event-Log und manuelle Steuerung |
| Datenbank | Speicherung von Zuständen, Events und optional Messwert-Verläufen |

## Entwicklungsworkflow
1. Zwei Teammitglieder testen Hardware jeweils mit einem eigenen ESP32 direkt per USB-C/USB am Laptop. Sensorik, Aktorik, Flashen und serielle Logs werden dabei lokal getestet. Solange nur die serielle Verbindung genutzt wird, spielen statische IP-Adressen keine Rolle.
2. Zwei Teammitglieder entwickeln parallel Backend, Frontend und Datenbank zunächst lokal mit Mock-Daten oder einem kleinen Simulator für Sensorwerte.
3. In der Integrationsphase werden Firmware, MQTT, REST, WebSocket und Hardware schrittweise zusammengeführt. Erst ab der Netzwerkintegration von ESP32 und Raspberry Pi sind statische IP-Adressen relevant.
4. Im Finalbetrieb läuft die Anwendung dann auf dem Raspberry Pi in 2 Docker-Containern: 1 Container für den MQTT-Broker und 1 Container für die Anwendung.

## Entwicklungsregeln Git
1. Es gibt die Branches `main` und `dev`.
2. Jeder arbeitet immer in einem eigenen Branch. Der Branchname beginnt immer mit dem eigenen Namen, z. B. `jan`.
3. Subbranches beginnen ebenfalls mit dem eigenen Namen und danach optional mit dem Feature, z. B. `jan-frontend` oder `jan-mqtt-fix`.
4. Es wird nicht direkt auf `main` gearbeitet.
5. Feature- und Personen-Branches werden zuerst in `dev` gemergt.
6. Erst wenn der Stand auf `dev` gemeinsam geprüft und vereinheitlicht wurde, wird von `dev` nach `main` gemergt.

## Datenfluss
1. Sensoren liefern Messwerte an den ESP32.
2. Der ESP32 verarbeitet Messwerte und sendet Zustände/Sensordaten per MQTT an den Raspberry Pi.
3. Das Spring Boot Backend verarbeitet die MQTT-Nachrichten und speichert relevante Daten in der Datenbank.
4. Das Backend überträgt Live-Daten per WebSocket an das Vue.js Frontend.
5. Steuerbefehle aus dem Frontend gehen per REST an das Backend.
6. Das Backend sendet die Steuerbefehle per MQTT an den ESP32.
7. Der ESP32 führt die Aktion aus und meldet den neuen Status erneut per MQTT zurück.
