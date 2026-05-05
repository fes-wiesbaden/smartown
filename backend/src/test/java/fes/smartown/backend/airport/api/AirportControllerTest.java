package fes.smartown.backend.airport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.airport.model.AirportMode;
import fes.smartown.backend.airport.model.AirportSnapshot;
import fes.smartown.backend.airport.model.AirportStatePayload;
import fes.smartown.backend.airport.service.AirportCommandPublisher;
import fes.smartown.backend.airport.service.AirportStateService;
import fes.smartown.backend.config.WebCorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AirportController.class)
@Import(WebCorsConfig.class)
class AirportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AirportStateService airportStateService;

    @MockBean
    private AirportCommandPublisher airportCommandPublisher;

    @Test
    void returnsCurrentAirportSnapshot() throws Exception {
        when(airportStateService.getSnapshot()).thenReturn(snapshot());

        mockMvc.perform(get("/api/airport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.mode").value("ON"))
                .andExpect(jsonPath("$.state.lightsOn").value(true))
                .andExpect(jsonPath("$.state.online").value(true))
                .andExpect(jsonPath("$.brokerConnected").value(true));
    }

    @Test
    void publishesAirportModeCommands() throws Exception {
        when(airportStateService.getSnapshot()).thenReturn(snapshot());

        mockMvc.perform(put("/api/airport/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mode", "OFF"))))
                .andExpect(status().isAccepted());

        verify(airportCommandPublisher).publishModeCommand(AirportMode.OFF);
    }

    @Test
    void returnsServiceUnavailableWhenMqttCommandFails() throws Exception {
        doThrow(new IllegalStateException("MQTT broker unavailable"))
                .when(airportCommandPublisher)
                .publishModeCommand(AirportMode.ON);

        mockMvc.perform(put("/api/airport/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mode", "ON"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void appliesSharedCorsRulesToAirportModeEndpoint() throws Exception {
        mockMvc.perform(options("/api/airport/mode")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PUT")));
    }

    private static AirportSnapshot snapshot() {
        return new AirportSnapshot(
                new AirportStatePayload(AirportMode.ON, true, true),
                true,
                Instant.parse("2026-05-05T08:00:00Z")
        );
    }
}
