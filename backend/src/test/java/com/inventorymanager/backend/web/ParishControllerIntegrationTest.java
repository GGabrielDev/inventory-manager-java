package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.Municipality;
import com.inventorymanager.backend.domain.Parish;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.ParishRepository;
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
class ParishControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParishRepository parishRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Parish savedParish;
    private Municipality savedMunicipality;

    @BeforeEach
    void setUp() {
        State state = new State();
        state.setName("Parish Test State");
        state = stateRepository.save(state);

        savedMunicipality = new Municipality();
        savedMunicipality.setName("Parish Test Municipality");
        savedMunicipality.setState(state);
        savedMunicipality = municipalityRepository.save(savedMunicipality);

        Parish p = new Parish();
        p.setName("Test Parish");
        p.setMunicipality(savedMunicipality);
        savedParish = parishRepository.save(p);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_parish"})
    void listReturnsPaginatedParishes() throws Exception {
        mockMvc.perform(get("/api/parishes?pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNumber())
                .andExpect(jsonPath("$.data[?(@.name == 'Test Parish')].id").exists());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_parish"})
    void listByMunicipalityReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/parishes?municipalityId=" + savedMunicipality.getId() + "&pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Parish"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_parish"})
    void getByIdReturnsParish() throws Exception {
        mockMvc.perform(get("/api/parishes/" + savedParish.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Parish"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_parish"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/parishes/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_parish"})
    void createParishSucceeds() throws Exception {
        CrudRequest.ParishUpsert request = new CrudRequest.ParishUpsert("New Parish", savedMunicipality.getId());
        mockMvc.perform(post("/api/parishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Parish"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_parish"})
    void createParishWithInvalidMunicipalityReturns400() throws Exception {
        CrudRequest.ParishUpsert request = new CrudRequest.ParishUpsert("Bad Parish", 99999L);
        mockMvc.perform(post("/api/parishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_parish"})
    void updateParishSucceeds() throws Exception {
        CrudRequest.ParishUpsert request = new CrudRequest.ParishUpsert("Updated Parish", savedMunicipality.getId());
        mockMvc.perform(put("/api/parishes/" + savedParish.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Parish"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_parish"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.ParishUpsert request = new CrudRequest.ParishUpsert("Updated Parish", savedMunicipality.getId());
        mockMvc.perform(put("/api/parishes/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_parish"})
    void deleteParishWithNoDependentsSucceeds() throws Exception {
        mockMvc.perform(delete("/api/parishes/" + savedParish.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_parish"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/parishes/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/parishes"))
                .andExpect(status().isForbidden());
    }
}
