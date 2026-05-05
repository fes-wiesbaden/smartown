package fes.smartown.backend.airport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.airport.model.AirportSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AirportRealtimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirportRealtimeService.class);

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public AirportRealtimeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public void sendSnapshot(WebSocketSession session, AirportSnapshot snapshot) {
        if (!session.isOpen()) {
            unregister(session);
            return;
        }

        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(snapshot)));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Could not serialize airport snapshot for websocket session {}", session.getId(), exception);
        } catch (IOException exception) {
            unregister(session);
            LOGGER.warn("Could not send airport snapshot to websocket session {}", session.getId(), exception);
        }
    }

    public void broadcast(AirportSnapshot snapshot) {
        sessions.forEach(session -> sendSnapshot(session, snapshot));
    }
}
