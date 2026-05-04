/**
 * KI-Hinweis:
 * Diese Testklasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend;

import fes.smartown.backend.lanterns.persistence.LanternDeviceRepository;
import fes.smartown.backend.lanterns.persistence.LanternLuxMeasurementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "smartown.mqtt.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class BackendApplicationTests {

    @MockBean
    private LanternDeviceRepository lanternDeviceRepository;

    @MockBean
    private LanternLuxMeasurementRepository lanternLuxMeasurementRepository;

    @Test
    void contextLoads() {
    }

}
