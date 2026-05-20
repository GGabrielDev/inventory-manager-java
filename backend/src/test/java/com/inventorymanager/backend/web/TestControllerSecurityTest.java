package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventorymanager.backend.config.AuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@Transactional
public class TestControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedAccessToDiagnosticsIsDenied() throws Exception {
        // Spring Security defaults to 403 Forbidden when unauthenticated and no custom entry point is defined
        mockMvc.perform(get("/api/test/health"))
               .andExpect(status().isForbidden());
               
        mockMvc.perform(get("/api/test/info"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user")
    void authenticatedAccessToDiagnosticsIsAllowed() throws Exception {
        mockMvc.perform(get("/api/test/health"))
               .andExpect(status().isOk());
               
        mockMvc.perform(get("/api/test/info"))
               .andExpect(status().isOk());
    }
}
