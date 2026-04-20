# MQTT fuer Laternen

[Ziel](#ziel) | [Topic-Schema](#topic-schema) | [Payload-Typen](#payload-typen) | [Bedeutung der Typen](#bedeutung-der-typen) | [Bedeutung der Felder](#bedeutung-der-felder) | [Bedeutung der Werte](#bedeutung-der-werte) | [Erlaubte Werte](#erlaubte-werte) | [Hinweise](#hinweise)

## Ziel
Dieses Dokument beschreibt das erste MQTT-Schema fuer das Modul `Laternen`.

## Topic-Schema
- `smartown/lanterns/command`
- `smartown/lanterns/state`
- `smartown/lanterns/event`

## Payload-Typen
`command`
```json
{
  "action": "SET_MODE",
  "mode": "AUTO"
}
```

`state`
```json
{
  "mode": "AUTO",
  "lightState": "ON",
  "lux": 12,
  "online": true
}
```

`event`
```json
{
  "type": "LIGHT_STATE_CHANGED",
  "lightState": "ON",
  "reason": "LOW_LUX"
}
```

## Bedeutung der Typen
- `command`: Steuerbefehl vom Backend an den ESP32
- `state`: aktueller Zustand vom ESP32 an das Backend
- `event`: fachlich relevantes Ereignis fuer Frontend, Logging oder Nachvollziehbarkeit

## Bedeutung der Felder
- `action`: konkrete Aktion, die der ESP32 ausfuehren soll
- `type`: fachlicher Name des Ereignisses
- `mode`: Betriebsmodus der Laternensteuerung
- `lightState`: aktueller Lichtzustand
- `lux`: aktuell gemessener Helligkeitswert des BH1750
- `online`: zeigt, ob der ESP32 aktuell erreichbar ist
- `reason`: Grund fuer ein Ereignis oder einen Zustandswechsel
- `thresholdLux`: konfigurierbarer Schwellwert fuer den Modus `AUTO`

## Bedeutung der Werte
- `AUTO`: Backend schaltet die Laternen anhand des Helligkeitswerts
- `FORCED_ON`: Laternen bleiben manuell eingeschaltet
- `FORCED_OFF`: Laternen bleiben manuell ausgeschaltet
- `ON`: Licht ist aktuell an
- `OFF`: Licht ist aktuell aus
- `LOW_LUX`: Umschaltung oder Pruefung wegen Dunkelheit
- `HIGH_LUX`: Umschaltung oder Pruefung wegen Helligkeit
- `MANUAL_OVERRIDE`: Zustand wurde manuell geaendert
- `SYSTEM_START`: Ereignis beim Start oder Neustart

## Erlaubte Werte
- `mode`: `AUTO`, `FORCED_ON`, `FORCED_OFF`
- `lightState`: `ON`, `OFF`
- `reason`: `LOW_LUX`, `HIGH_LUX`, `MANUAL_OVERRIDE`, `SYSTEM_START`

## Hinweise
- `mode` und `lightState` sind bewusst getrennt. `AUTO` beschreibt die Steuerlogik, `ON` oder `OFF` den realen Lampenzustand.
- `thresholdLux` ist Teil der Konfiguration, auch wenn es im ersten Minimalbeispiel oben noch nicht auftaucht.
