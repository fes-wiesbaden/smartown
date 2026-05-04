package fes.smartown.backend.bridge.service;

public interface BridgeCommandPublisher {
    /**
     * Sendet einen Befehl ("OPEN" oder "CLOSE") an die Brücke.
     */
    void publishCommand(String command);
}
