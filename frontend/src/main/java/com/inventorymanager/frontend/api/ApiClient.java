package com.inventorymanager.frontend.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ApiClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private String token;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void login(String username, String password) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Login failed: " + response.body());
        }
        Map<String, String> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
        this.token = payload.get("token");
    }

    public Map<String, Object> me() throws IOException, InterruptedException {
        HttpResponse<String> response = sendAuthorized("/auth/me", "GET", null);
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    public List<Map<String, Object>> list(String resource) throws IOException, InterruptedException {
        HttpResponse<String> response = sendAuthorized("/" + resource + "?page=1&pageSize=100", "GET", null);
        Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
        Object data = payload.get("data");
        return objectMapper.convertValue(data, new TypeReference<>() {});
    }

    public Map<String, Object> create(String resource, Map<String, Object> body) throws IOException, InterruptedException {
        HttpResponse<String> response = sendAuthorized("/" + resource, "POST", objectMapper.writeValueAsString(body));
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    public Map<String, Object> get(String resource) throws IOException, InterruptedException {
        HttpResponse<String> response = sendAuthorized("/" + resource, "GET", null);
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    public Map<String, Object> update(String resource, Number id, Map<String, Object> body) throws IOException, InterruptedException {
        HttpResponse<String> response = sendAuthorized("/" + resource + "/" + id, "PUT", objectMapper.writeValueAsString(body));
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    public void delete(String resource, Number id) throws IOException, InterruptedException {
        sendAuthorized("/" + resource + "/" + id, "DELETE", null);
    }

    private HttpResponse<String> sendAuthorized(String path, String method, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");
        switch (method) {
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            case "DELETE" -> builder.DELETE();
            default -> builder.GET();
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Request failed (" + response.statusCode() + "): " + response.body());
        }
        return response;
    }
}
