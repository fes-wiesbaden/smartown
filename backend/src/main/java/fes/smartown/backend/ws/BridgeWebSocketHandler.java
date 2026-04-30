package fes.smartown.backend.ws;

import fes.smartown.backend.bridge.service.BridgeRealtimeService;
import fes.smartown.backend.bridge.service.BridgeStateService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
/**
 * Bindet das Brueckenmodul an den reinen Snapshot-Stream nach aussen an.
 */
public class BridgeWebSocketHandler extends TextWebSocketHandler {

    private final BridgeRealtimeService bridgeRealtimeService;
    private final BridgeStateService bridgeStateService;

    public BridgeWebSocketHandler(BridgeRealtimeService bridgeRealtimeService,
                                  BridgeStateService bridgeStateService) {
        this.bridgeRealtimeService = bridgeRealtimeService;
        this.bridgeStateService = bridgeStateService;
    }

    @Override
    /**
     * Registriert neue Clients und liefert sofort den letzten bekannten Zustand.
     */
    public void afterConnectionEstablished(WebSocketSession session) {
        bridgeRealtimeService.register(session);
        bridgeRealtimeService.sendSnapshot(session, bridgeStateService.getSnapshot());
    }

    @Override
    /**
     * Commands bleiben bewusst auf REST beschraenkt.
     */
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }

    @Override
    /**
     * Raeumt geschlossene Sessions wieder aus dem Broadcast-Register auf.
     */
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        bridgeRealtimeService.unregister(session);
    }
}
