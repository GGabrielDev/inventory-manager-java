package com.inventorymanager.frontend.ui.views;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventorymanager.frontend.ui.UIUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class FormViewBagEditPayloadTest {

    @Test
    void testBagCreateSendsExpectedItems() {
        UIUtils.IdName branch = new UIUtils.IdName(1L, "Branch");
        UIUtils.IdName dept = new UIUtils.IdName(2L, "Dept");
        
        Map<String, Object> payload = FormView.constructBagPayload(false, "Bag 1", "BAR123", branch, dept);
        
        assertTrue(payload.containsKey("expectedItems"), "Create payload should contain expectedItems to initialize the list");
        assertTrue(((java.util.List<?>) payload.get("expectedItems")).isEmpty(), "Initial expectedItems should be empty list");
        assertEquals("Bag 1", payload.get("name"));
    }

    @Test
    void testBagEditOmitsExpectedItems() {
        UIUtils.IdName branch = new UIUtils.IdName(1L, "Branch");
        UIUtils.IdName dept = new UIUtils.IdName(2L, "Dept");
        
        Map<String, Object> payload = FormView.constructBagPayload(true, "Bag 1 Edited", "BAR123", branch, dept);
        
        assertFalse(payload.containsKey("expectedItems"), "Edit payload must NOT contain expectedItems to avoid clearing existing items");
        assertEquals("Bag 1 Edited", payload.get("name"));
    }
}
