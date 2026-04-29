package fes.smartown.backend.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
/**
 * Registriert die WebSocket-Endpunkte fuer Live-Updates im Laternen-MVP.
 */
public class LanternWebSocketConfig implements WebSocketConfigurer {

    private final LanternWebSocketHandler lanternWebSocketHandler;

    public LanternWebSocketConfig(LanternWebSocketHandler lanternWebSocketHandler) {
        this.lanternWebSocketHandler = lanternWebSocketHandler;
    }

    @Override
    /**
     * Bindet den Laternen-Handler an den oeffentlichen WebSocket-Pfad.
     */
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(lanternWebSocketHandler, "/ws/lanterns")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://10.93.128.204:*"
                );
    }
}
