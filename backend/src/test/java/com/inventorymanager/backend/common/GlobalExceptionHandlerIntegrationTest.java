package com.inventorymanager.backend.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventorymanager.backend.config.AuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({AuditingConfig.class, GlobalExceptionHandlerIntegrationTest.TestConfig.class})
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestConfig {
        @RestController
        static class BuggyController {
            @GetMapping("/api/test/bug")
            public void triggerBug() {
                throw new RuntimeException("KABOOM");
            }
        }
    }

    @Test
    @WithMockUser
    void testEnhancedErrorPayload() throws Exception {
        mockMvc.perform(get("/api/test/bug"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.backendError").value("RuntimeException"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.path").value("/api/test/bug"))
                .andExpect(jsonPath("$.method").value("GET"));
    }
}
