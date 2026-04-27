package com.inventorymanager.frontend.api;

import static org.junit.jupiter.api.Assertions.*;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import java.util.Map;

class ApiClientTest {

    private MockWebServer mockWebServer;
    private ApiClient apiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        apiClient = new ApiClient(mockWebServer.url("/api").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void meParsesRolesCorrectly() throws Exception {
        // Mock a response that matches the backend /api/auth/me structure
        String json = """
            {
                "id": 1,
                "username": "admin",
                "roles": ["admin", "operator"],
                "permissions": ["get_item", "edit_item"]
            }
            """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> result = apiClient.me();

        assertEquals("admin", result.get("username"));
        
        // This is what caused the ClassCastException in the UI
        Object roles = result.get("roles");
        assertTrue(roles instanceof List, "Roles should be a list");
        
        @SuppressWarnings("unchecked")
        List<Object> rolesList = (List<Object>) roles;
        assertFalse(rolesList.isEmpty());
        assertTrue(rolesList.get(0) instanceof String, "Role element should be a String");
        assertEquals("admin", rolesList.get(0));
    }
}
