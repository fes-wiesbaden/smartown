-- KI-Hinweis:
-- Diese Migration wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
-- Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
-- Die Migration wurde projektbezogen geprüft und validiert.
UPDATE lantern_device
SET controller_ip_address = '10.93.135.232'
WHERE device_key = 'LANTERN_SENSOR_CONTROLLER';
