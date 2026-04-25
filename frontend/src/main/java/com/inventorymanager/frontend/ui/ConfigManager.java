package com.inventorymanager.frontend.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles local configuration for the desktop application.
 * Stores settings like the backend API URL in the user's home directory.
 */
public class ConfigManager {
    private static final String CONFIG_DIR = ".inventory-manager";
    private static final String CONFIG_FILE = "config.json";
    private static final String DEFAULT_URL = "http://localhost:4000/api";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path configPath;
    private Map<String, String> config = new HashMap<>();

    public ConfigManager() {
        this.configPath = Paths.get(getConfigDirectory(), CONFIG_DIR, CONFIG_FILE);
        load();
    }

    private String getConfigDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData != null ? appData : userHome;
        } else if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support").toString();
        } else {
            // Linux/Unix fallback to XDG Base Directory specification or home
            String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfigHome != null && !xdgConfigHome.isEmpty()) {
                return xdgConfigHome;
            }
            return Paths.get(userHome, ".config").toString();
        }
    }

    public String getApiUrl() {
        return config.getOrDefault("apiUrl", DEFAULT_URL);
    }

    public void setApiUrl(String url) {
        config.put("apiUrl", url);
        save();
    }

    private void load() {
        if (Files.exists(configPath)) {
            try {
                config = objectMapper.readValue(configPath.toFile(), new TypeReference<>() {});
            } catch (IOException e) {
                System.err.println("Failed to load config: " + e.getMessage());
            }
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), config);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
}
