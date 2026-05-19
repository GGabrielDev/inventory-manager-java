package com.inventorymanager.backend.audit;

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
class AuditLogControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", authorities = {"get_audit_logs"})
    void auditLogsAccessibleWithAuthority() throws Exception {
        mockMvc.perform(get("/api/audit-logs/Item/1"))
               .andExpect(status().isOk());
               
        mockMvc.perform(get("/api/audit-logs/Item"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"create_item", "edit_item"})
    void auditLogsDeniedWithoutAuthority() throws Exception {
        mockMvc.perform(get("/api/audit-logs/Item/1"))
               .andExpect(status().isForbidden());
               
        mockMvc.perform(get("/api/audit-logs/Item"))
               .andExpect(status().isForbidden());
    }

    @Test
    void auditLogsRequireSpecificAuthorityReflection() {
        // Architectural verification: Check if @PreAuthorize exists on the method.
        try {
            var method1 = AuditLogController.class.getMethod("getAuditTrail", String.class, Long.class, int.class, int.class);
            var preAuth1 = method1.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            if (preAuth1 == null || !preAuth1.value().contains("get_audit_logs")) {
                throw new RuntimeException("VIOLATION: Missing get_audit_logs authority check on getAuditTrail");
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
