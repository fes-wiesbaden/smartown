package fes.smartown.backend.ws;

import fes.smartown.backend.airport.service.AirportRealtimeService;
import fes.smartown.backend.airport.service.AirportStateService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class AirportWebSocketHandler extends TextWebSocketHandler {

    private final AirportRealtimeService airportRealtimeService;
    private final AirportStateService airportStateService;

    public AirportWebSocketHandler(AirportRealtimeService airportRealtimeService,
                                   AirportStateService airportStateService) {
        this.airportRealtimeService = airportRealtimeService;
        this.airportStateService = airportStateService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        airportRealtimeService.register(session);
        airportRealtimeService.sendSnapshot(session, airportStateService.getSnapshot());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        airportRealtimeService.unregister(session);
    }
}
