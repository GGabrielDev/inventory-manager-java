package com.inventorymanager.backend.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AuditLogControllerBoundaryTest {

    private MockMvc mockMvc;
    private AuditService auditService;

    @BeforeEach
    public void setUp() {
        auditService = Mockito.mock(AuditService.class);
        AuditLogController controller = new AuditLogController(auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testPageSizeUpperBoundary() throws Exception {
        mockMvc.perform(get("/api/audit-logs/Item")
                .param("pageSize", "500"))
                .andExpect(status().isOk());
        
        // Should be capped at 100
        Mockito.verify(auditService).auditTrail("Item", null, 0, 100);
    }

    @Test
    public void testPageSizeLowerBoundary() throws Exception {
        mockMvc.perform(get("/api/audit-logs/Item")
                .param("pageSize", "0"))
                .andExpect(status().isOk());
        
        // Should fallback to 10
        Mockito.verify(auditService).auditTrail("Item", null, 0, 10);
    }
}
