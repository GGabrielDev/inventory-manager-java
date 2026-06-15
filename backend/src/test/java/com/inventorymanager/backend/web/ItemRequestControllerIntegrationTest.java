package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ItemRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private ParishRepository parishRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Branch savedBranch;
    private Department savedDepartment;

    @BeforeEach
    void setUp() {
        State state = new State();
        state.setName("IR Test State");
        state = stateRepository.save(state);

        Municipality muni = new Municipality();
        muni.setName("IR Test Muni");
        muni.setState(state);
        muni = municipalityRepository.save(muni);

        Parish parish = new Parish();
        parish.setName("IR Test Parish");
        parish.setMunicipality(muni);
        parish = parishRepository.save(parish);

        savedBranch = new Branch();
        savedBranch.setName("IR Test Branch");
        savedBranch.setAddress("789 IR St");
        savedBranch.setState(state);
        savedBranch.setMunicipality(muni);
        savedBranch.setParish(parish);
        savedBranch = branchRepository.save(savedBranch);

        savedDepartment = new Department();
        savedDepartment.setName("IR Test Dept");
        savedDepartment.setBranch(savedBranch);
        savedDepartment = departmentRepository.save(savedDepartment);
    }

    @Test
    @WithUserDetails("admin")
    void createDraftRequestSucceeds() throws Exception {
        CrudRequest.ItemRequestEntryUpsert entry = new CrudRequest.ItemRequestEntryUpsert(
                null, "Test Item", 5, Item.UnitType.UND,
                null, savedDepartment.getId(), null,
                "Test observations", null);

        CrudRequest.ItemRequestUpsert request = new CrudRequest.ItemRequestUpsert(
                ItemRequestType.INBOUND, "Test Request", "Testing item requests",
                List.of(entry), savedBranch.getId());

        mockMvc.perform(post("/api/item-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Request"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.requestType").value("INBOUND"))
                .andExpect(jsonPath("$.entries[0].requestedItemName").value("Test Item"))
                .andExpect(jsonPath("$.entries[0].requestedQuantity").value(5));
    }

    @Test
    @WithUserDetails("admin")
    void createRequestWithoutEntriesReturns400() throws Exception {
        CrudRequest.ItemRequestUpsert request = new CrudRequest.ItemRequestUpsert(
                ItemRequestType.INBOUND, "Empty", "No entries",
                List.of(), savedBranch.getId());

        mockMvc.perform(post("/api/item-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_item_request"})
    void listReturnsData() throws Exception {
        mockMvc.perform(get("/api/item-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_item_request"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/item-requests/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/item-requests"))
                .andExpect(status().isForbidden());
    }
}
