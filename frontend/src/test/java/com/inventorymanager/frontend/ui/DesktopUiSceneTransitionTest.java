package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.inventorymanager.frontend.api.ApiClient;
import java.util.List;
import java.util.Map;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

public class DesktopUiSceneTransitionTest extends ApplicationTest {

    @BeforeAll
    static void setupHeadless() {
        // Handled by xvfb-run in CI
    }

    static class MockApiClient extends ApiClient {
        public MockApiClient() {
            super("http://dummy");
        }
        @Override
        public Map<String, Object> me() {
            return Map.of(
                "username", "testuser",
                "permissions", List.of("create_user", "get_audit_logs", "create_branch", "create_department", 
                                       "create_category", "create_user", "create_role", "create_permission", "create_state", 
                                       "create_municipality", "create_parish")
            );
        }
    }

    static class MockConfigManager extends ConfigManager {
        @Override
        public String getLanguage() { return "en"; }
    }

    @Override
    public void start(Stage stage) {
        // Required by TestFX
    }

    @Test
    void testShowDashboardTransitionDoesNotThrowRootViolation() throws Exception {
        MockApiClient mockApi = new MockApiClient();
        MockConfigManager mockConfig = new MockConfigManager();

        interact(() -> {
            Stage stage = new Stage();
            // This constructor creates the first scene with mainLayout as root
            DesktopUi ui = new DesktopUi(stage, mockApi, mockConfig);
            
            // showDashboard should now REUSE the existing scene/root instead of throwing
            assertDoesNotThrow(() -> {
                try {
                    java.lang.reflect.Method method = DesktopUi.class.getDeclaredMethod("showDashboard");
                    method.setAccessible(true);
                    method.invoke(ui);
                } catch (Exception e) {
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            }, "Transitioning to dashboard should not throw root violation error after fix");
            
            stage.close();
        });
    }

    @Test
    void testRootOwnershipInvariant() {
        interact(() -> {
            javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
            javafx.scene.Scene scene1 = new javafx.scene.Scene(root);
            
            // Assert that creating a second scene with the same root throws the expected FX error
            // This PROVES that our fix (not creating a new Scene) is necessary
            assertThrows(IllegalArgumentException.class, () -> {
                new javafx.scene.Scene(root);
            }, "JavaFX must throw if same root is assigned to multiple scenes");
        });
    }
}
