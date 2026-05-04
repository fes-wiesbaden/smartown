package fes.smartown.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "smartown.mqtt.enabled=false")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
