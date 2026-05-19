package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.inventorymanager.frontend.api.ApiClient;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.ResourceBundle;

public class FrontendStartupSmokeTest {

    @BeforeAll
    static void initJfx() {
        // Removed Platform.startup() to avoid "Unable to open DISPLAY" in headless CI.
        // Basic component construction (new BorderPane, etc.) may still work depending on environment.
    }

    static class MockApiClient extends ApiClient {
        public MockApiClient() {
            super("http://dummy");
        }
    }

    static class MockConfigManager extends ConfigManager {
        @Override
        public String getLanguage() { return "en"; }
    }

    @Test
    void testAppStartupFlowDoesNotNPE() throws Exception {
        // Use manual mocks instead of Mockito to avoid Java 25 compatibility issues
        ApiClient mockApi = new MockApiClient();
        ConfigManager mockConfig = new MockConfigManager();

        // Initialize UI (this used to cause NPE in constructor or immediate showLogin)
        // We pass null for Stage since we only want to test internal field initialization
        DesktopUi ui = new DesktopUi(null, mockApi, mockConfig);
        
        // Assert that core layout containers are initialized (fixing P0 NPE)
        java.lang.reflect.Field mainLayoutField = DesktopUi.class.getDeclaredField("mainLayout");
        mainLayoutField.setAccessible(true);
        assertNotNull(mainLayoutField.get(ui), "mainLayout should be initialized in constructor");

        java.lang.reflect.Field contentAreaField = DesktopUi.class.getDeclaredField("contentArea");
        contentAreaField.setAccessible(true);
        assertNotNull(contentAreaField.get(ui), "contentArea should be initialized in constructor");
        
        assertNotNull(ui);
    }
}
