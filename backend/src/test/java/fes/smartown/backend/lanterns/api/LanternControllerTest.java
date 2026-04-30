package fes.smartown.backend.lanterns.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.lanterns.model.LanternEventPayload;
import fes.smartown.backend.lanterns.model.LanternMode;
import fes.smartown.backend.lanterns.model.LanternReason;
import fes.smartown.backend.lanterns.model.LanternSnapshot;
import fes.smartown.backend.lanterns.model.LanternStatePayload;
import fes.smartown.backend.lanterns.model.LightState;
import fes.smartown.backend.lanterns.service.LanternCommandPublisher;
import fes.smartown.backend.lanterns.service.LanternStateService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LanternController.class)
/**
 * Prueft die REST-Vertraege des Laternen-Controllers isoliert vom MQTT-Broker.
 */
class LanternControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LanternStateService lanternStateService;

    @MockBean
    private LanternCommandPublisher lanternCommandPublisher;

    @Test
    /**
     * Erwartet, dass der Snapshot unveraendert ueber GET ausgeliefert wird.
     */
    void returnsCurrentLanternSnapshot() throws Exception {
        when(lanternStateService.getSnapshot()).thenReturn(snapshot());

        mockMvc.perform(get("/api/lanterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.mode").value("AUTO"))
                .andExpect(jsonPath("$.state.lightState").value("ON"))
                .andExpect(jsonPath("$.lastEvent.reason").value("LOW_LUX"))
                .andExpect(jsonPath("$.brokerConnected").value(true));
    }

    @Test
    /**
     * Erwartet, dass ein REST-Moduswechsel als fachliches Command publiziert wird.
     */
    void publishesLanternModeCommands() throws Exception {
        when(lanternStateService.getSnapshot()).thenReturn(snapshot());

        mockMvc.perform(put("/api/lanterns/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("mode", "FORCED_OFF"))))
                .andExpect(status().isAccepted());

        ArgumentCaptor<LanternMode> captor = ArgumentCaptor.forClass(LanternMode.class);
        verify(lanternCommandPublisher).publishModeCommand(captor.capture());
        assertThat(captor.getValue()).isEqualTo(LanternMode.FORCED_OFF);
    }

    @Test
    /**
     * Erwartet einen 503-Fehler, wenn der MQTT-Versand aus dem Controller scheitert.
     */
    void returnsServiceUnavailableWhenMqttCommandFails() throws Exception {
        doThrow(new IllegalStateException("MQTT broker unavailable"))
                .when(lanternCommandPublisher)
                .publishModeCommand(LanternMode.FORCED_ON);

        mockMvc.perform(put("/api/lanterns/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("mode", "FORCED_ON"))))
                .andExpect(status().isServiceUnavailable());
    }

    /**
     * Baut einen festen Snapshot fuer die Web-Layer-Tests.
     */
    private static LanternSnapshot snapshot() {
        return new LanternSnapshot(
                new LanternStatePayload(LanternMode.AUTO, LightState.ON, 12.0, true, 50.0),
                new LanternEventPayload("LIGHT_STATE_CHANGED", LightState.ON, LanternReason.LOW_LUX),
                true,
                Instant.parse("2026-04-23T08:00:00Z")
        );
    }
}
