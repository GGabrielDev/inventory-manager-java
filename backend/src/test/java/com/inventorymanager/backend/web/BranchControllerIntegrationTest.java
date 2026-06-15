package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.*;
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
class BranchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private ParishRepository parishRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Branch savedBranch;
    private State savedState;
    private Municipality savedMunicipality;
    private Parish savedParish;

    @BeforeEach
    void setUp() {
        savedState = stateRepository.save(createState("Branch Test State"));
        savedMunicipality = municipalityRepository.save(createMunicipality("Branch Test Muni", savedState));
        savedParish = parishRepository.save(createParish("Branch Test Parish", savedMunicipality));

        Branch b = new Branch();
        b.setName("Test Branch");
        b.setAddress("123 Test St");
        b.setState(savedState);
        b.setMunicipality(savedMunicipality);
        b.setParish(savedParish);
        savedBranch = branchRepository.save(b);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_branch"})
    void listReturnsPaginatedBranches() throws Exception {
        mockMvc.perform(get("/api/branches?pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNumber())
                .andExpect(jsonPath("$.data[?(@.name == 'Test Branch')].address").value("123 Test St"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_branch"})
    void listByStateReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/branches?stateId=" + savedState.getId() + "&pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Branch"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_branch"})
    void getByIdReturnsBranch() throws Exception {
        mockMvc.perform(get("/api/branches/" + savedBranch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Branch"))
                .andExpect(jsonPath("$.address").value("123 Test St"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_branch"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/branches/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_branch"})
    void createBranchSucceeds() throws Exception {
        CrudRequest.BranchUpsert request = new CrudRequest.BranchUpsert(
                "New Branch", "456 New St",
                savedState.getId(), savedMunicipality.getId(), savedParish.getId());
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Branch"))
                .andExpect(jsonPath("$.address").value("456 New St"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_branch"})
    void createBranchWithInvalidStateReturns400() throws Exception {
        CrudRequest.BranchUpsert request = new CrudRequest.BranchUpsert(
                "Bad Branch", "456 Bad St",
                99999L, savedMunicipality.getId(), savedParish.getId());
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_branch"})
    void updateBranchSucceeds() throws Exception {
        CrudRequest.BranchUpsert request = new CrudRequest.BranchUpsert(
                "Updated Branch", "789 Updated St",
                savedState.getId(), savedMunicipality.getId(), savedParish.getId());
        mockMvc.perform(put("/api/branches/" + savedBranch.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Branch"))
                .andExpect(jsonPath("$.address").value("789 Updated St"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_branch"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.BranchUpsert request = new CrudRequest.BranchUpsert(
                "Updated Branch", "789 Updated St",
                savedState.getId(), savedMunicipality.getId(), savedParish.getId());
        mockMvc.perform(put("/api/branches/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_branch"})
    void deleteBranchWithoutDependentsSucceeds() throws Exception {
        // Branch creation auto-creates departments; we must delete them first
        // or the delete will fail with 409 conflict
        mockMvc.perform(delete("/api/branches/" + savedBranch.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_branch"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/branches/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isForbidden());
    }

    private static State createState(String name) {
        State s = new State();
        s.setName(name);
        return s;
    }

    private static Municipality createMunicipality(String name, State state) {
        Municipality m = new Municipality();
        m.setName(name);
        m.setState(state);
        return m;
    }

    private static Parish createParish(String name, Municipality muni) {
        Parish p = new Parish();
        p.setName(name);
        p.setMunicipality(muni);
        return p;
    }
}
