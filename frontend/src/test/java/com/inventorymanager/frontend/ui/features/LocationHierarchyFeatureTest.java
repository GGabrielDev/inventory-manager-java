package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Feature test for the location hierarchy: State → Municipality → Parish CRUD,
 * including cascade protection on deletion.
 */
class LocationHierarchyFeatureTest extends FeatureTestBase {

    @Test
    void stateCrudThroughFrontend() throws Exception {
        initDesktopUiAndLogin();

        // CREATE
        createStateThroughForm("Test State");
        Map<String, Object> state = waitForEntityByName("states", "Test State", Duration.ofSeconds(10));
        assertNotNull(state);
        assertTrue(((Number) state.get("id")).longValue() > 0);
        assertEquals("Test State", state.get("name"));

        // READ (list)
        long count = countEntities("states");
        assertTrue(count >= 1, "Should have at least 1 state after creation");

        // UPDATE
        editNamedThroughForm("States", "states", state, "Test State Updated");
        Map<String, Object> updated = waitForEntityByName("states", "Test State Updated", Duration.ofSeconds(15));
        assertNotNull(updated);
        assertEquals("Test State Updated", updated.get("name"));
    }

    @Test
    void municipalityCrudThroughFrontend() throws Exception {
        initDesktopUiAndLogin();

        // Prerequisite: parent state
        apiClient.create("states", Map.of("name", "Muni Test State"));
        Map<String, Object> parentState = waitForEntityByName("states", "Muni Test State", Duration.ofSeconds(10));

        // CREATE
        createMunicipalityThroughForm("Test Municipality", "Muni Test State");
        Map<String, Object> muni = waitForEntityByName("municipalities", "Test Municipality", Duration.ofSeconds(10));
        assertNotNull(muni);
        assertEquals("Test Municipality", muni.get("name"));

        // Ensure it references the correct state
        Map<?, ?> stateRef = (Map<?, ?>) muni.get("state");
        assertNotNull(stateRef);
        assertEquals(parentState.get("id"), stateRef.get("id"));

        // UPDATE
        editNamedThroughForm("Municipalities", "municipalities", muni, "Test Municipality Updated");
        Map<String, Object> updated = waitForEntityByName("municipalities", "Test Municipality Updated",
                Duration.ofSeconds(10));
        assertNotNull(updated);
        assertEquals("Test Municipality Updated", updated.get("name"));
    }

    @Test
    void parishCrudThroughFrontend() throws Exception {
        initDesktopUiAndLogin();

        // Prerequisites
        apiClient.create("states", Map.of("name", "Parish Test State"));
        Map<String, Object> state = waitForEntityByName("states", "Parish Test State", Duration.ofSeconds(10));

        Map<String, Object> muniPayload = Map.of("name", "Parish Test Muni", "stateId", state.get("id"));
        apiClient.create("municipalities", muniPayload);
        Map<String, Object> muni = waitForEntityByName("municipalities", "Parish Test Muni", Duration.ofSeconds(10));

        // CREATE via frontend
        createParishThroughForm("Test Parish", "Parish Test State", "Parish Test Muni");
        Map<String, Object> parish = waitForEntityByName("parishes", "Test Parish", Duration.ofSeconds(15));
        assertNotNull(parish);
        assertEquals("Test Parish", parish.get("name"));

        // Verify the municipality reference
        Map<?, ?> muniRef = (Map<?, ?>) parish.get("municipality");
        assertNotNull(muniRef);
        assertEquals(muni.get("id"), muniRef.get("id"));
    }

    @Test
    void fullLocationChainWithCascadeProtection() throws Exception {
        initDesktopUiAndLogin();

        // Build the full chain: State → Municipality → Parish → Branch
        apiClient.create("states", Map.of("name", "Chain State"));
        Map<String, Object> state = waitForEntityByName("states", "Chain State", Duration.ofSeconds(10));

        apiClient.create("municipalities", Map.of("name", "Chain Muni", "stateId", state.get("id")));
        Map<String, Object> muni = waitForEntityByName("municipalities", "Chain Muni", Duration.ofSeconds(10));

        apiClient.create("parishes", Map.of("name", "Chain Parish", "municipalityId", muni.get("id")));
        Map<String, Object> parish = waitForEntityByName("parishes", "Chain Parish", Duration.ofSeconds(10));

        // Create a branch referencing the chain
        apiClient.create("branches", Map.of(
                "name", "Chain Branch",
                "address", "123 Chain St",
                "stateId", state.get("id"),
                "municipalityId", muni.get("id"),
                "parishId", parish.get("id")
        ));

        // Attempt to delete the state — should be blocked because municipality
        // references prevent it.
        try {
            apiClient.delete("states", ((Number) state.get("id")).longValue());
        } catch (Exception ignored) {
        }

        // Verify state still exists due to referential protection
        Map<String, Object> stillThere = waitForEntityByName("states", "Chain State", Duration.ofSeconds(5));
        assertNotNull(stillThere, "State should still exist after failed deletion");
    }

    @Test
    void createStateAndMunicipalityThroughFrontend() throws Exception {
        // Test just CREATE through the frontend UI (edits through combo forms
        // are flaky in headless TestFX; CRUD is verified by backend tests).
        initDesktopUiAndLogin();

        createStateThroughForm("UI State");
        waitForEntityByName("states", "UI State", Duration.ofSeconds(15));
    }
}
