package com.inventorymanager.backend.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnostic endpoints for authenticated callers.
 * <p>
 * These routes are intentionally auth-gated and are used for liveness checks,
 * parameter echoing, and lightweight runtime metadata.
 * </p>
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "Diagnostics", description = "Endpoints for system health checks and testing connectivity")
public class TestController {

    /**
     * Simple health check endpoint for authenticated callers.
     *
     * @return A map containing the status and current server time.
     */
    @Operation(
        summary = "Health Check",
        description = "Returns the current status of the application along with the server timestamp for an authenticated caller.",
        tags = { "Diagnostics" }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is up and running",
            content = { @Content(mediaType = "application/json",
            schema = @Schema(example = "{\"status\": \"UP\", \"timestamp\": \"2024-05-20T10:00:00\"}")) }
        )
    })
    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "message", "Inventory Manager Backend is responsive"
        ));
    }

    /**
     * Echo endpoint for testing request parameters and connectivity.
     *
     * @param message The message to be echoed back.
     * @return A map containing the echoed message.
     */
    @Operation(
        summary = "Echo Test",
        description = "Echoes back the provided message parameter to verify parameter passing and connectivity for an authenticated caller.",
        tags = { "Diagnostics" }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully echoed the message",
            content = { @Content(mediaType = "application/json",
            schema = @Schema(example = "{\"echo\": \"Hello World\", \"length\": 11}")) }
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content
        )
    })
    @GetMapping("/echo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> echo(
        @Parameter(description = "The message to echo back", example = "Hello World")
        @RequestParam(defaultValue = "ping") String message
    ) {
        return ResponseEntity.ok(Map.of(
            "echo", message,
            "length", message.length(),
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * System info endpoint providing basic details about the environment.
     *
     * @return Basic system information.
     */
    @Operation(
        summary = "System Information",
        description = "Provides basic information about the running environment (non-sensitive) for an authenticated caller.",
        tags = { "Diagnostics" }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "System information retrieved successfully",
            content = { @Content(mediaType = "application/json",
            schema = @Schema(example = "{\"java_version\": \"21\", \"os_name\": \"Linux\"}")) }
        )
    })
    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> info() {
        return ResponseEntity.ok(Map.of(
            "application_name", "Inventory Manager Backend",
            "version", "2.0.0",
            "status", "OPERATIONAL"
        ));
    }
}
