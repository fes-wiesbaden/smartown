package fes.smartown.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/**
 * Erlaubt bekannte Entwicklungs-Hosts fuer die gesamte REST-API.
 */
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    /**
     * Gibt den bekannten Entwicklungs-Hosts Zugriff auf alle API-Endpunkte.
     */
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://10.93.128.204:*"
                )
                .allowedMethods("GET", "PUT", "POST", "OPTIONS");
    }
}
