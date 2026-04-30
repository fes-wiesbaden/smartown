package fes.smartown.backend.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
/**
 * Registriert alle technischen WebSocket-Endpunkte zentral an einer Stelle.
 */
public class WebSocketConfig implements WebSocketConfigurer {

    private static final String[] ALLOWED_ORIGIN_PATTERNS = {
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://10.93.128.204:*"
    };

    private final LanternWebSocketHandler lanternWebSocketHandler;
    private final BridgeWebSocketHandler bridgeWebSocketHandler;

    public WebSocketConfig(LanternWebSocketHandler lanternWebSocketHandler,
                           BridgeWebSocketHandler bridgeWebSocketHandler) {
        this.lanternWebSocketHandler = lanternWebSocketHandler;
        this.bridgeWebSocketHandler = bridgeWebSocketHandler;
    }

    @Override
    /**
     * Haengt jedes Modul an seinen oeffentlichen WebSocket-Pfad.
     */
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registerHandler(registry, lanternWebSocketHandler, "/ws/lanterns");
        registerHandler(registry, bridgeWebSocketHandler, "/ws/bridge");
    }

    /**
     * Zentraler Helper, damit Origin-Regeln und Handler-Registrierung konsistent bleiben.
     */
    private void registerHandler(WebSocketHandlerRegistry registry, WebSocketHandler handler, String path) {
        registry.addHandler(handler, path).setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
    }
}
