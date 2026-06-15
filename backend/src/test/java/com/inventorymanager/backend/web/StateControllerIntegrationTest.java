package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.repository.StateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class StateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private State savedState;

    @BeforeEach
    void setUp() {
        State s = new State();
        s.setName("Test State");
        savedState = stateRepository.save(s);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_state"})
    void listReturnsPaginatedStates() throws Exception {
        mockMvc.perform(get("/api/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.id == " + savedState.getId() + ")].name").value("Test State"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_state"})
    void getByIdReturnsState() throws Exception {
        mockMvc.perform(get("/api/states/" + savedState.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test State"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_state"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/states/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_state"})
    void createStateSucceeds() throws Exception {
        CrudRequest.NamedUpsert request = new CrudRequest.NamedUpsert("New State");
        mockMvc.perform(post("/api/states")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New State"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_state"})
    void updateStateSucceeds() throws Exception {
        CrudRequest.NamedUpsert request = new CrudRequest.NamedUpsert("Updated State");
        mockMvc.perform(put("/api/states/" + savedState.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated State"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_state"})
    void deleteStateWithNoDependentsSucceeds() throws Exception {
        mockMvc.perform(delete("/api/states/" + savedState.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/states"))
                .andExpect(status().isForbidden());
    }
}
