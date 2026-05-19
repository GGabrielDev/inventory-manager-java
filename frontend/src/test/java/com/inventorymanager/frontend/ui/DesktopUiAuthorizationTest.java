package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventorymanager.frontend.api.ApiClient;
import java.util.List;
import java.util.Map;
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
    void testIsAdminRequiresAllCorePermissions() throws Exception {
        MockApiClient mockClient = new MockApiClient(Map.of(
            "roles", List.of("admin"),
            "permissions", List.of("create_user", "get_audit_logs", "create_branch", "create_department")
        ));
        
        DesktopUi ui = new DesktopUi(null, mockClient, new MockConfigManager());
        
        java.lang.reflect.Method method = DesktopUi.class.getDeclaredMethod("showDashboard");
        method.setAccessible(true);
        
        try {
            method.invoke(ui);
        } catch (Exception e) {
            // Expected
        }

        java.lang.reflect.Field field = DesktopUi.class.getDeclaredField("isAdmin");
        field.setAccessible(true);
        boolean isAdmin = (boolean) field.get(ui);
        
        assertFalse(isAdmin, "Should require all core permissions");
    }

    @Test
    void testIsAdminWithAllCorePermissions() throws Exception {
        MockApiClient mockClient = new MockApiClient(Map.of(
            "roles", List.of("operator"),
            "permissions", List.of(
                "get_audit_logs", "create_branch", "create_department", "create_category", 
                "create_user", "create_role", "create_permission", "create_state", 
                "create_municipality", "create_parish"
            )
        ));
        
        DesktopUi ui = new DesktopUi(null, mockClient, new MockConfigManager());
        
        java.lang.reflect.Method method = DesktopUi.class.getDeclaredMethod("showDashboard");
        method.setAccessible(true);
        
        try {
            method.invoke(ui);
        } catch (Exception e) {
            // Expected
        }

        java.lang.reflect.Field field = DesktopUi.class.getDeclaredField("isAdmin");
        field.setAccessible(true);
        boolean isAdmin = (boolean) field.get(ui);
        
        assertTrue(isAdmin, "Should be admin when all core permissions are present");
    }
}
