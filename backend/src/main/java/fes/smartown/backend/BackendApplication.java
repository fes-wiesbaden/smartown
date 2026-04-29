package fes.smartown.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
/**
 * Startpunkt der Backend-Anwendung inklusive Konfigurationsscan fuer MQTT-Properties.
 */
public class BackendApplication {

    /**
     * Startet den Spring-Boot-Kontext fuer Backend, MQTT-Bruecke und Webschnittstellen.
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
