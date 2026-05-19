package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventorymanager.frontend.api.ApiClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class DesktopUiAuthorizationTest {

    static class MockApiClient extends ApiClient {
        private final Map<String, Object> mockMeData;

        public MockApiClient(Map<String, Object> mockMeData) {
            super("http://dummy");
            this.mockMeData = mockMeData;
        }

        @Override
        public Map<String, Object> me() {
            return mockMeData;
        }
    }

    static class MockConfigManager extends ConfigManager {
        @Override
        public String getLanguage() { return "en"; }
        @Override
        public String getApiUrl() { return "http://dummy"; }
    }

    @Test
    void testIsAdminRequiresAllCorePermissions() {
        Set<String> permissions = Set.of("create_user", "get_audit_logs", "create_branch", "create_department");
        boolean isAdmin = DesktopUi.computeIsAdmin(permissions);
        assertFalse(isAdmin, "Should require all core permissions");
    }

    @Test
    void testIsAdminWithAllCorePermissions() {
        Set<String> permissions = Set.of(
            "get_audit_logs", "create_branch", "create_department", "create_category", 
            "create_user", "create_role", "create_permission", "create_state", 
            "create_municipality", "create_parish"
        );
        boolean isAdmin = DesktopUi.computeIsAdmin(permissions);
        assertTrue(isAdmin, "Should be admin when all core permissions are present");
    }
}
