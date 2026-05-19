package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class UIUtilsTest {

    @Test
    public void testParseBackendError() {
        String json = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Something went wrong\",\"details\":\"Stack trace\\nline 1\",\"timestamp\":\"2026-05-18\"}";
        Exception ex = new Exception("Request failed (500): " + json);
        
        UIUtils.ErrorReport report = UIUtils.parseErrorReport("Original Fail", ex);
        
        assertEquals("Something went wrong", report.displayMessage);
        assertEquals("Stack trace\nline 1", report.backendDetails);
    }

    @Test
    public void testParseBackendErrorFallback() {
        Exception ex = new Exception("Plain error");
        UIUtils.ErrorReport report = UIUtils.parseErrorReport("Original Fail", ex);
        
        assertEquals("Original Fail", report.displayMessage);
        assertEquals("", report.backendDetails);
    }
}
