package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Feature test for Category CRUD through the frontend.
 */
class CategoryFeatureTest extends FeatureTestBase {

    @Test
    void categoryCrudThroughFrontend() throws Exception {
        initDesktopUiAndLogin();

        // CREATE
        createNamedThroughForm("Categories", "categories", "Test Category");
        Map<String, Object> category = waitForEntityByName("categories", "Test Category", Duration.ofSeconds(10));
        assertNotNull(category);
        assertTrue(((Number) category.get("id")).longValue() > 0);
        assertEquals("Test Category", category.get("name"));

        // UPDATE
        editNamedThroughForm("Categories", "categories", category, "Test Category Updated");
        Map<String, Object> updated = waitForEntityByName("categories", "Test Category Updated",
                Duration.ofSeconds(10));
        assertNotNull(updated);
        assertEquals("Test Category Updated", updated.get("name"));

        // DELETE
        category = updated;
        // Branches auto-create Inbound + Storage depts. We can safely delete a
        // standalone category.
        apiClient.delete("categories", ((Number) category.get("id")).longValue());
        waitForEntityAbsent("categories", "Test Category Updated", Duration.ofSeconds(10));
        assertEquals(0, countEntities("categories"), "Categories should be empty after deletion");
    }
}
