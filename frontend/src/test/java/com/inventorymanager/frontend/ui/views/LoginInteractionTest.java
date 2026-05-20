package com.inventorymanager.frontend.ui.views;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.ConfigManager;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class LoginInteractionTest {

    static class MockApiClient extends ApiClient {
        private boolean loginCalled = false;

        public MockApiClient() {
            super("http://dummy");
        }

        @Override
        public void login(String u, String p) throws java.io.IOException, InterruptedException {
            loginCalled = true;
        }

        public boolean wasLoginCalled() {
            return loginCalled;
        }
    }

    static class MockConfigManager extends ConfigManager {
        @Override
        public String getLanguage() { return "en"; }
    }

    static class MockResourceBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String key) {
            return "test";
        }

        @Override
        public java.util.Enumeration<String> getKeys() {
            return java.util.Collections.emptyEnumeration();
        }
    }

    @Test
    void testLoginTriggersDashboardShower() throws Exception {
        MockApiClient mockApi = new MockApiClient();
        ResourceBundle mockBundle = new MockResourceBundle();
        
        CountDownLatch latch = new CountDownLatch(1);
        Runnable mockDashboardShower = latch::countDown;

        ViewContext context = new ViewContext(
            mockApi, mockBundle, new MockConfigManager(),
            node -> {}, () -> {}, mockDashboardShower,
            () -> {}, (t, r) -> {}
        );

        LoginView loginView = new LoginView(context);
        loginView.setUiThreadExecutor(Runnable::run); // STABILIZATION: Execute immediately on same thread in tests
        
        // Call performLogin directly (no DISPLAY needed)
        loginView.performLogin("admin", "password");
        
        boolean reached = latch.await(2, TimeUnit.SECONDS);
        assertTrue(reached, "Dashboard shower should be called after successful login");
        assertTrue(mockApi.wasLoginCalled(), "login() should have been called on ApiClient");
    }

    @Test
    void testFailedLoginDoesNotTriggerDashboard() throws Exception {
        MockApiClient mockApi = new MockApiClient() {
            @Override
            public void login(String u, String p) throws java.io.IOException {
                throw new java.io.IOException("Invalid credentials");
            }
        };
        ResourceBundle mockBundle = new MockResourceBundle();
        
        CountDownLatch latch = new CountDownLatch(1);
        Runnable mockDashboardShower = latch::countDown;

        ViewContext context = new ViewContext(
            mockApi, mockBundle, new MockConfigManager(),
            node -> {}, () -> {}, mockDashboardShower,
            () -> {}, (t, r) -> {}
        );

        LoginView loginView = new LoginView(context);
        loginView.setUiThreadExecutor(Runnable::run);
        
        loginView.performLogin("admin", "wrong");
        
        boolean reached = latch.await(1, TimeUnit.SECONDS);
        assertFalse(reached, "Dashboard shower should NOT be called after failed login");
    }
}
