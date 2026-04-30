package fes.smartown.backend.bridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.bridge.model.BridgeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * Verteilt Bruecken-Snapshots an alle verbundenen WebSocket-Clients.
 */
public class BridgeRealtimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeRealtimeService.class);

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public BridgeRealtimeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Merkt sich eine neue Session fuer spaetere Broadcasts.
     */
    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    /**
     * Entfernt geschlossene oder defekte Sessions aus dem Register.
     */
    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    /**
     * Sendet einen Snapshot gezielt an eine Session.
     */
    public void sendSnapshot(WebSocketSession session, BridgeSnapshot snapshot) {
        if (!session.isOpen()) {
            unregister(session);
            return;
        }

        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(snapshot)));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Could not serialize bridge snapshot for websocket session {}", session.getId(), exception);
        } catch (IOException exception) {
            unregister(session);
            LOGGER.warn("Could not send bridge snapshot to websocket session {}", session.getId(), exception);
        }
    }

    /**
     * Broadcastet den aktuellen Snapshot an alle aktiven Sessions.
     */
    public void broadcast(BridgeSnapshot snapshot) {
        sessions.forEach(session -> sendSnapshot(session, snapshot));
    }
}
