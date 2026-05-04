/**
 * KI-Hinweis:
 * Diese Testklasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.lanterns.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class LanternFlywayMigrationTest {

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.8");

    @Test
    void seedsLanternControllerWithConfiguredHostIpAddress() throws SQLException {
        Flyway.configure()
                .dataSource(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword())
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                mariadb.getJdbcUrl(),
                mariadb.getUsername(),
                mariadb.getPassword()
        );
             var statement = connection.prepareStatement("""
                     SELECT host_ip_address, mqtt_broker_port, sampling_interval_minutes
                     FROM lantern_device
                     WHERE device_key = ?
                     """)) {
            statement.setString(1, "LANTERN_SENSOR_CONTROLLER");

            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("host_ip_address")).isEqualTo("10.93.135.232");
                assertThat(resultSet.getInt("mqtt_broker_port")).isEqualTo(1883);
                assertThat(resultSet.getInt("sampling_interval_minutes")).isEqualTo(5);
                assertThat(resultSet.next()).isFalse();
            }
        }
    }
}
