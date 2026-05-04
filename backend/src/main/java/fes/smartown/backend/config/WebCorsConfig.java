package fes.smartown.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/**
 * Erlaubt lokale Frontend-Aufrufe auf die REST-API waehrend des MQTT-MVPs.
 */
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    /**
     * Gibt den bekannten Entwicklungs-Hosts Zugriff auf die Laternen-API.
     */
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://10.93.128.204:*"
                )
                .allowedMethods("GET", "PUT", "OPTIONS");
    }
}
