package com.inventorymanager.frontend.api;

import static org.junit.jupiter.api.Assertions.*;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import java.nio.charset.StandardCharsets;
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
    void listAppendsParamsOnlyIfMissing() throws Exception {
        String json = "{\"data\": []}";
        
        // Case 1: No params provided
        mockWebServer.enqueue(new MockResponse().setBody(json).addHeader("Content-Type", "application/json"));
        apiClient.list("items");
        RecordedRequest request1 = mockWebServer.takeRequest();
        assertTrue(request1.getPath().contains("page=1"), "Should append page=1 if missing");

        // Case 2: Params already provided
        mockWebServer.enqueue(new MockResponse().setBody(json).addHeader("Content-Type", "application/json"));
        apiClient.list("items?custom=true");
        RecordedRequest request2 = mockWebServer.takeRequest();
        assertFalse(request2.getPath().contains("page=1"), "Should NOT append default params if '?' exists");
        assertTrue(request2.getPath().contains("custom=true"));
    }

    @Test
    void meParsesRolesCorrectly() throws Exception {
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
        
        Object roles = result.get("roles");
        assertTrue(roles instanceof List, "Roles should be a list");
        
        @SuppressWarnings("unchecked")
        List<Object> rolesList = (List<Object>) roles;
        assertFalse(rolesList.isEmpty());
        assertTrue(rolesList.get(0) instanceof String, "Role element should be a String");
        assertEquals("admin", rolesList.get(0));
    }

    @Test
    void loginPostsCredentialsAndStoresToken() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"token\":\"abc123\"}")
                .addHeader("Content-Type", "application/json"));

        apiClient.login("admin", "password");

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/auth/login", request.getPath());
        String body = request.getBody().readString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"username\":\"admin\""));
        assertTrue(body.contains("\"password\":\"password\""));

        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":1}")
                .addHeader("Content-Type", "application/json"));
        apiClient.me();
        RecordedRequest authRequest = mockWebServer.takeRequest();
        assertEquals("Bearer abc123", authRequest.getHeader("Authorization"));
    }
}
