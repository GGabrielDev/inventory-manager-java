package com.inventorymanager.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
@EnableJpaAuditing
public class InventoryManagerBackendApplication {
    
    public static void main(String[] args) {
        String host = getEnv("DB_HOST", "localhost");
        String port = getEnv("DB_PORT", "5432");
        String dbName = getEnv("DB_NAME", "inventory_manager_java");
        String user = getEnv("DB_USER", "postgres");
        String pass = getEnv("DB_PASSWORD", "");
        String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);

        if (!checkDatabaseConnection(url, user, pass)) {
            System.err.println("========================================================================");
            System.err.println("ERROR: DATABASE CONNECTION FAILED");
            System.err.println("========================================================================");
            System.err.println("The application could not connect to the PostgreSQL database.");
            System.err.println("Please ensure that:");
            System.err.println("  1. PostgreSQL is installed and running.");
            System.err.println(String.format("  2. The database '%s' exists.", dbName));
            System.err.println("  3. Credentials in application.yml (or DB_USER/DB_PASSWORD) are correct.");
            System.err.println("");
            System.err.println(String.format("Attempted connection: %s", url));
            System.err.println("========================================================================");
            System.exit(1);
        }

        try {
            SpringApplication.run(InventoryManagerBackendApplication.class, args);
        } catch (Exception e) {
            handleStartupException(e);
        }
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    private static boolean checkDatabaseConnection(String url, String user, String pass) {
        try (Connection ignored = DriverManager.getConnection(url, user, pass)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void handleStartupException(Exception e) {
        // Fallback for other startup errors
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        
        System.err.println("Application failed to start: " + cause.getMessage());
        System.exit(1);
    }
}
