package com.inventorymanager.backend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class GlobalExceptionHandlerLogicTest {

    @Test
    public void testHandleUnexpectedWithNullMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception ex = new Exception((String) null);
        
        ResponseEntity<Map<String, Object>> response = handler.handleUnexpected(ex);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error", response.getBody().get("message"));
    }

    @Test
    public void testHandleApiExceptionWithNullMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ApiException ex = new ApiException(HttpStatus.BAD_REQUEST, null);
        
        ResponseEntity<Map<String, Object>> response = handler.handleApiException(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No message available", response.getBody().get("message"));
    }

    @Test
    public void testHandleBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalArgumentException ex = new IllegalArgumentException("Bad input");
        
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad input", response.getBody().get("message"));
    }

    @Test
    public void testHandleForbidden() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.security.access.AccessDeniedException ex = new org.springframework.security.access.AccessDeniedException("No access");
        
        ResponseEntity<Map<String, Object>> response = handler.handleForbidden(ex);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No access", response.getBody().get("message"));
    }
}
