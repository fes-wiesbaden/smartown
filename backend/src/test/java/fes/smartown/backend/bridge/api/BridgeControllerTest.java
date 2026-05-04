/**
 * KI-Hinweis:
 * Diese Testklasse wurde mit Unterstützung von KI angefertigt und/oder überarbeitet.
 * Verwendete Werkzeuge: https://www.claude.ai und https://www.chatgpt.com
 * Der Code wurde projektbezogen geprüft und validiert.
 */
package fes.smartown.backend.bridge.api;

import fes.smartown.backend.bridge.model.BridgeMode;
import fes.smartown.backend.bridge.model.BridgeSnapshot;
import fes.smartown.backend.bridge.service.BridgeStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BridgeController.class)
@Import(fes.smartown.backend.config.WebCorsConfig.class)
class BridgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BridgeStateService bridgeStateService;

    @Test
    void returnsCurrentBridgeSnapshot() throws Exception {
        when(bridgeStateService.getSnapshot()).thenReturn(
                new BridgeSnapshot(
                        BridgeMode.AUTO,
                        false,
                        true,
                        true,
                        Instant.parse("2026-04-23T08:00:00Z")
                )
        );

        mockMvc.perform(get("/api/bridge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("AUTO"))
                .andExpect(jsonPath("$.isPhysicallyOpen").value(false))
                .andExpect(jsonPath("$.brokerConnected").value(true))
                .andExpect(jsonPath("$.espOnline").value(true));
    }

    @Test
    void acceptsBridgeModeUpdatesViaPost() throws Exception {
        mockMvc.perform(post("/api/bridge/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"MANUAL_OPEN\"}"))
                .andExpect(status().isAccepted());

        verify(bridgeStateService).setMode(BridgeMode.MANUAL_OPEN);
    }

    @Test
    void appliesSharedCorsRulesToBridgeModeEndpoint() throws Exception {
        mockMvc.perform(options("/api/bridge/mode")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
    }
}
