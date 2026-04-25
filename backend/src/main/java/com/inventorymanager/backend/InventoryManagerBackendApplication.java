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
        if (!checkDatabaseConnection()) {
            System.err.println("========================================================================");
            System.err.println("ERROR: DATABASE CONNECTION FAILED");
            System.err.println("========================================================================");
            System.err.println("The application could not connect to the PostgreSQL database.");
            System.err.println("Please ensure that:");
            System.err.println("  1. PostgreSQL is installed and running.");
            System.err.println("  2. The database 'inventory_manager_java' exists.");
            System.err.println("  3. Credentials in application.yml (or DB_USER/DB_PASSWORD) are correct.");
            System.err.println("");
            System.err.println("Default expected connection: jdbc:postgresql://localhost:5432/inventory_manager_java");
            System.err.println("========================================================================");
            System.exit(1);
        }

        try {
            SpringApplication.run(InventoryManagerBackendApplication.class, args);
        } catch (Exception e) {
            handleStartupException(e);
        }
    }

    private static boolean checkDatabaseConnection() {
        // We manually check connectivity before Spring starts to provide a better error message.
        // These values match the defaults in application.yml
        String host = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
        String port = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "5432";
        String dbName = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "inventory_manager_java";
        String user = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
        String pass = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "postgres";
        
        String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);

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
