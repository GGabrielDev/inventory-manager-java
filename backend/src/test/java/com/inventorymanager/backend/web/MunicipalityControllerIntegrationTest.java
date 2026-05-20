package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.Municipality;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.StateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@Transactional
class MunicipalityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_municipality"})
    void testUpdateMunicipality() throws Exception {
        State state = new State();
        state.setName("Test State");
        state = stateRepository.save(state);

        Municipality municipality = new Municipality();
        municipality.setName("Old Name");
        municipality.setState(state);
        municipality = municipalityRepository.save(municipality);

        CrudRequest.MunicipalityUpsert request = new CrudRequest.MunicipalityUpsert("New Name", state.getId());

        mockMvc.perform(put("/api/municipalities/" + municipality.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.state.id").value(state.getId()));
    }
}
