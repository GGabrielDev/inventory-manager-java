package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Feature test for Department CRUD through the frontend.
 * Departments are linked to a Branch.
 */
class DepartmentFeatureTest extends FeatureTestBase {

    @Test
    void departmentCrudThroughFrontend() throws Exception {
        initDesktopUiAndLogin();

        // Prerequisite: a branch (we'll create one via API for speed)
        apiClient.create("states", Map.of("name", "Dept Test State"));
        Map<String, Object> state = waitForEntityByName("states", "Dept Test State", Duration.ofSeconds(10));
        apiClient.create("municipalities", Map.of("name", "Dept Test Muni", "stateId", state.get("id")));
        Map<String, Object> muni = waitForEntityByName("municipalities", "Dept Test Muni", Duration.ofSeconds(10));
        apiClient.create("parishes", Map.of("name", "Dept Test Parish", "municipalityId", muni.get("id")));
        Map<String, Object> parish = waitForEntityByName("parishes", "Dept Test Parish", Duration.ofSeconds(10));

        apiClient.create("branches", Map.of(
                "name", "Dept Test Branch",
                "address", "789 Dept St",
                "stateId", state.get("id"),
                "municipalityId", muni.get("id"),
                "parishId", parish.get("id")
        ));
        Map<String, Object> branch = waitForEntityByName("branches", "Dept Test Branch", Duration.ofSeconds(10));

        // CREATE
        createDepartmentThroughForm("Test Department", "Dept Test Branch");
        Map<String, Object> dept = waitForEntityByName("departments", "Test Department", Duration.ofSeconds(10));
        assertNotNull(dept);
        assertEquals("Test Department", dept.get("name"));

        // Verify branch reference
        Map<?, ?> branchRef = (Map<?, ?>) dept.get("branch");
        assertNotNull(branchRef);
        assertEquals(branch.get("id"), branchRef.get("id"));
    }
}
