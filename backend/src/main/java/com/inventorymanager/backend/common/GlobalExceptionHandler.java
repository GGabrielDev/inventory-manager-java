package com.inventorymanager.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final org.springframework.core.env.Environment env;

    public GlobalExceptionHandler(org.springframework.core.env.Environment env) {
        this.env = env;
    }

    private Map<String, Object> createErrorBody(HttpStatus status, String message, Exception ex, WebRequest request) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message != null ? message : "No message available");
        
        if (request instanceof ServletWebRequest) {
            HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
            body.put("path", servletRequest.getRequestURI());
            body.put("method", servletRequest.getMethod());
        }

        if (ex != null) {
            body.put("backendError", ex.getClass().getSimpleName());
            
            // STABILIZATION: Provide detailed stack trace in dev/test profiles for faster triage
            boolean isDev = java.util.Arrays.asList(env.getActiveProfiles()).contains("dev") || 
                            java.util.Arrays.asList(env.getActiveProfiles()).contains("test");
            if (isDev) {
                java.io.StringWriter sw = new java.io.StringWriter();
                ex.printStackTrace(new java.io.PrintWriter(sw));
                body.put("details", sw.toString());
            } else {
                body.put("details", ex.getMessage());
            }
        }
        return body;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException exception, WebRequest request) {
        return ResponseEntity.status(exception.getStatus())
                .body(createErrorBody(exception.getStatus(), exception.getMessage(), null, request));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorBody(HttpStatus.BAD_REQUEST, exception.getMessage(), null, request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException exception, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorBody(HttpStatus.FORBIDDEN, exception.getMessage(), null, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "An unexpected error occurred. Please contact support.", exception, request));
    }
}
