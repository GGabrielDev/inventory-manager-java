package com.inventorymanager.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
@EnableJpaAuditing
public class InventoryManagerBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryManagerBackendApplication.class, args);
    }
}
