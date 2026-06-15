package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Feature test for Audit Log viewing.
 * Audit logs record all entity changes via JaVers.
 */
class AuditLogFeatureTest extends FeatureTestBase {

    @Test
    void createStateProducesAuditLog() throws Exception {
        initDesktopUiAndLogin();

        // Create a state
        apiClient.create("states", Map.of("name", "Audit Test State"));
        Map<String, Object> state = waitForEntityByName("states", "Audit Test State", Duration.ofSeconds(10));
        long stateId = ((Number) state.get("id")).longValue();

        // Query audit logs for this state
        Map<String, Object> auditLogs = apiClient.listRaw("audit-logs/state/" + stateId);
        assertNotNull(auditLogs);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) auditLogs.get("data");
        assertNotNull(data, "Audit logs should contain data");
        assertFalse(data.isEmpty(), "At least one audit entry should exist for the created state");

        // Verify audit entry has required fields
        Map<String, Object> entry = data.get(0);
        assertNotNull(entry.get("operation"), "Audit entry should have operation");
        assertNotNull(entry.get("changedBy"), "Audit entry should have changedBy");
        assertNotNull(entry.get("changedAt"), "Audit entry should have timestamp");
    }

    @Test
    void updateAndDeleteProduceAuditTrail() throws Exception {
        initDesktopUiAndLogin();

        // Create category
        apiClient.create("categories", Map.of("name", "Audit Cat"));
        Map<String, Object> cat = waitForEntityByName("categories", "Audit Cat", Duration.ofSeconds(10));
        long catId = ((Number) cat.get("id")).longValue();

        // Update
        apiClient.update("categories", catId, Map.of("name", "Audit Cat Renamed"));
        waitForEntityByName("categories", "Audit Cat Renamed", Duration.ofSeconds(10));

        // Delete
        apiClient.delete("categories", catId);
        waitForEntityAbsent("categories", "Audit Cat Renamed", Duration.ofSeconds(10));

        // Query audit logs — should see INITIAL, UPDATE, and DELETE operations
        Map<String, Object> auditLogs = apiClient.listRaw("audit-logs/category/" + catId);
        assertNotNull(auditLogs);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) auditLogs.get("data");
        assertNotNull(data);
        assertTrue(data.size() >= 3, "Should have at least 3 audit entries for create + update + delete");
    }

    @Test
    void listAllAuditLogsForEntityType() throws Exception {
        initDesktopUiAndLogin();

        // Create some states
        apiClient.create("states", Map.of("name", "Audit List State A"));
        apiClient.create("states", Map.of("name", "Audit List State B"));
        waitForEntityByName("states", "Audit List State A", Duration.ofSeconds(10));
        waitForEntityByName("states", "Audit List State B", Duration.ofSeconds(10));

        // Query all audit logs for 'state' entity type
        Map<String, Object> auditLogs = apiClient.listRaw("audit-logs/state");
        assertNotNull(auditLogs);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) auditLogs.get("data");
        assertNotNull(data);
        assertTrue(data.size() >= 2, "Should have at least 2 audit entries for state creations");
    }
}
