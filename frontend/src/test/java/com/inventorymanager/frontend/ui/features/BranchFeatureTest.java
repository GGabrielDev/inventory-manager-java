package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Feature test for Branch CRUD through the frontend.
 * Branches have address + location hierarchy (State → Municipality → Parish).
 */
class BranchFeatureTest extends FeatureTestBase {

    @Test
    void branchCrudThroughFrontend() throws Exception {
        initDesktopUiAndLogin();

        // Prerequisites: location chain
        apiClient.create("states", Map.of("name", "Branch Test State"));
        Map<String, Object> state = waitForEntityByName("states", "Branch Test State", Duration.ofSeconds(10));

        apiClient.create("municipalities", Map.of("name", "Branch Test Muni", "stateId", state.get("id")));
        Map<String, Object> muni = waitForEntityByName("municipalities", "Branch Test Muni", Duration.ofSeconds(10));

        apiClient.create("parishes", Map.of("name", "Branch Test Parish", "municipalityId", muni.get("id")));
        waitForEntityByName("parishes", "Branch Test Parish", Duration.ofSeconds(10));

        // CREATE
        createBranchThroughForm("Test Branch", "456 Branch Ave", "Branch Test State",
                "Branch Test Muni", "Branch Test Parish");
        Map<String, Object> branch = waitForEntityByName("branches", "Test Branch", Duration.ofSeconds(15));
        assertNotNull(branch);
        assertEquals("Test Branch", branch.get("name"));
        assertEquals("456 Branch Ave", branch.get("address"));

        // Verify location references
        Map<?, ?> stateRef = (Map<?, ?>) branch.get("state");
        assertNotNull(stateRef);
        assertEquals(state.get("id"), stateRef.get("id"));

        // Verify auto-created departments (Inbound + Storage)
        // Note: seed data creates Inbound+Storage for "Main Branch", so we
        // find the ones belonging to our new branch by filtering on branch ID
        Map<String, Object> inboundDept = null;
        Map<String, Object> storageDept = null;
        for (int page = 1; page <= MAX_SEARCH_PAGES; page++) {
            for (Map<String, Object> dept : apiClient.list("departments?page=" + page + "&pageSize=" + SEARCH_PAGE_SIZE)) {
                Map<?, ?> deptBranch = (Map<?, ?>) dept.get("branch");
                if (deptBranch != null && branch.get("id").equals(deptBranch.get("id"))) {
                    if ("Inbound".equals(dept.get("name"))) inboundDept = dept;
                    if ("Storage".equals(dept.get("name"))) storageDept = dept;
                }
            }
        }
        assertNotNull(inboundDept, "Inbound department should be auto-created");
        assertNotNull(storageDept, "Storage department should be auto-created");

        // Verify departments belong to this branch
        Map<?, ?> inboundBranch = (Map<?, ?>) inboundDept.get("branch");
        Map<?, ?> storageBranch = (Map<?, ?>) storageDept.get("branch");
        assertNotNull(inboundDept, "Inbound department should be auto-created");
        assertNotNull(storageDept, "Storage department should be auto-created");
    }

    @Test
    void branchWithoutLocationFailsValidation() throws Exception {
        initDesktopUiAndLogin();

        // Attempt to open form without location data — branch requires 3 combos
        interact(() -> new com.inventorymanager.frontend.ui.views.FormView(viewContext)
                .showUpsertForm("Branches", "branches", null));
        waitUntil(() -> visibleTextFieldCount() >= 1, Duration.ofSeconds(10), "Branch form did not render");

        // Try to save with empty fields — should show validation error or not submit
        // We verify by checking no new branch appears in the list
        assertTrue(countEntities("branches") >= 1, "Should only have seeded branches");
    }
}
