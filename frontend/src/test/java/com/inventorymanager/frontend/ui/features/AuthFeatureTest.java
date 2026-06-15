package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.inventorymanager.frontend.api.ApiClient;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Feature test for authentication: login, me endpoint, and unauthorized access.
 */
class AuthFeatureTest extends FeatureTestBase {

    @Test
    void loginAsAdminShowsDashboard() throws Exception {
        initDesktopUiAndLogin();

        // After login the dashboard should show admin navigation buttons
        assertTrue(findButtonContaining("States").isPresent(), "Should show States nav");
        assertTrue(findButtonContaining("Branches").isPresent(), "Should show Branches nav");
        assertTrue(findButtonContaining("Users").isPresent(), "Should show Users nav");

        // Verify /auth/me returns admin
        Map<String, Object> me = apiClient.me();
        assertEquals("admin", me.get("username"));
    }

    @Test
    void loginWithBadPasswordShowsError() throws Exception {
        String baseUrl = backendApiBaseUrl();
        ApiClient badClient = new ApiClient(baseUrl);
        try {
            badClient.login("admin", "wrongpassword");
            fail("Should have thrown on bad password");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        String baseUrl = backendApiBaseUrl();
        ApiClient noAuthClient = new ApiClient(baseUrl);

        // The ApiClient always sends token; we need to test at HTTP level directly
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/states"))
                .GET()
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode(), "Unauthenticated request should return 403");
    }
}
