package com.inventorymanager.backend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

public class GlobalExceptionHandlerLogicTest {

    private GlobalExceptionHandler handler;
    private Environment env;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        env = mock(Environment.class);
        request = mock(WebRequest.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});
        handler = new GlobalExceptionHandler(env);
    }

    @Test
    public void testHandleUnexpectedWithNullMessage() {
        Exception ex = new Exception((String) null);
        
        ResponseEntity<Map<String, Object>> response = handler.handleUnexpected(ex, request);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred. Please contact support.", response.getBody().get("message"));
    }

    @Test
    public void testHandleApiExceptionWithNullMessage() {
        ApiException ex = new ApiException(HttpStatus.BAD_REQUEST, null);
        
        ResponseEntity<Map<String, Object>> response = handler.handleApiException(ex, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No message available", response.getBody().get("message"));
    }

    @Test
    public void testHandleBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad input");
        
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad input", response.getBody().get("message"));
    }

    @Test
    public void testHandleForbidden() {
        org.springframework.security.access.AccessDeniedException ex = new org.springframework.security.access.AccessDeniedException("No access");
        
        ResponseEntity<Map<String, Object>> response = handler.handleForbidden(ex, request);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No access", response.getBody().get("message"));
    }
}
