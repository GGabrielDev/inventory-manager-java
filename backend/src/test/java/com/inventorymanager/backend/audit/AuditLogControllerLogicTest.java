package com.inventorymanager.backend.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AuditLogControllerLogicTest {

    private MockMvc mockMvc;
    private AuditService auditService;

    @BeforeEach
    public void setup() {
        auditService = Mockito.mock(AuditService.class);
        AuditLogController controller = new AuditLogController(auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testPageSizeUpperBoundary() throws Exception {
        mockMvc.perform(get("/api/audit-logs/Item/1")
                .param("pageSize", "200"))
                .andExpect(status().isOk());
        
        // Should be capped at 100
        Mockito.verify(auditService).auditTrail("Item", 1L, 0, 100);
    }

    @Test
    public void testPageSizeLowerBoundary() throws Exception {
        mockMvc.perform(get("/api/audit-logs/Item/1")
                .param("pageSize", "0"))
                .andExpect(status().isOk());
        
        // Should fallback to 10
        Mockito.verify(auditService).auditTrail("Item", 1L, 0, 10);
    }

    @Test
    public void testPageUnderflow() throws Exception {
        mockMvc.perform(get("/api/audit-logs/Item/1")
                .param("page", String.valueOf(Integer.MIN_VALUE)))
                .andExpect(status().isOk());
        
        // Should cap at page 0
        Mockito.verify(auditService).auditTrail("Item", 1L, 0, 10);
    }
}
