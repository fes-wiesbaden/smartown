-- KI-Hinweis:
-- Diese Migration wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
-- Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
-- Die Migration wurde projektbezogen geprüft und validiert.
ALTER TABLE lantern_device
    CHANGE controller_ip_address host_ip_address VARCHAR(45) NULL;
