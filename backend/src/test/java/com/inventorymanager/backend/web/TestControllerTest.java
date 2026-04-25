package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit test for TestController using standalone MockMvc.
 * This avoids loading the full Spring context (JPA, Security, etc.)
 * which is problematic in the current Java 25 environment with Mockito/ByteBuddy.
 */
class TestControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController()).build();
    }

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/test/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Inventory Manager Backend is responsive"));
    }

    @Test
    void echoReturnsMessage() throws Exception {
        mockMvc.perform(get("/api/test/echo").param("message", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echo").value("hello"))
                .andExpect(jsonPath("$.length").value(5));
    }

    @Test
    void infoReturnsSystemInfo() throws Exception {
        mockMvc.perform(get("/api/test/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application_name").value("Inventory Manager Backend"))
                .andExpect(jsonPath("$.version").value("1.0.0-SNAPSHOT"));
    }
}
