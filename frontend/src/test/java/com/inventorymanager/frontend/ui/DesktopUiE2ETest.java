package com.inventorymanager.frontend.ui;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

import com.inventorymanager.frontend.api.ApiClient;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.api.FxRobot;
import java.util.List;
import java.util.Map;

@ExtendWith(ApplicationExtension.class)
class DesktopUiE2ETest {

    private ApiClient mockApiClient;
    private ConfigManager mockConfigManager;
    private DesktopUi ui;

    @Start
    void start(Stage stage) {
        mockApiClient = Mockito.mock(ApiClient.class);
        mockConfigManager = Mockito.mock(ConfigManager.class);
        ui = new DesktopUi(stage, mockApiClient, mockConfigManager);
        ui.showLogin();
    }

    @Test
    void testLoginFlow(FxRobot robot) throws Exception {
        // Setup mock responses
        Mockito.when(mockApiClient.me()).thenReturn(Map.of(
            "username", "admin",
            "roles", List.of("admin")
        ));

        // Interaction
        robot.clickOn("Sign In");

        // Verify transition to Dashboard
        // We look for the brand label or any unique dashboard element
        verifyThat(".label", hasText("INV MANAGER 2.0"));
        verifyThat("User: admin", hasText("User: admin"));
    }
}
