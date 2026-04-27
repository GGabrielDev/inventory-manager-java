package com.inventorymanager.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.Item;
import com.inventorymanager.backend.web.CrudRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@Transactional
class AcceptanceScenarioTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", authorities = {"get_item", "create_displacement", "get_displacement", "create_state", "create_municipality", "create_parish", "create_branch", "create_department", "create_item"})
    void scenarioZeroPaperworkBorrowing() throws Exception {
        // Setup Prerequisite Data
        String stateJson = objectMapper.writeValueAsString(Map.of("name", "Scenario State"));
        MvcResult stateResult = mockMvc.perform(post("/api/states").contentType(MediaType.APPLICATION_JSON).content(stateJson)).andReturn();
        Long stateId = ((Number) objectMapper.readValue(stateResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        String muniJson = objectMapper.writeValueAsString(new CrudRequest.MunicipalityUpsert("Scenario Muni", stateId));
        MvcResult muniResult = mockMvc.perform(post("/api/municipalities").contentType(MediaType.APPLICATION_JSON).content(muniJson)).andReturn();
        Long muniId = ((Number) objectMapper.readValue(muniResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        String parishJson = objectMapper.writeValueAsString(new CrudRequest.ParishUpsert("Scenario Parish", muniId));
        MvcResult parishResult = mockMvc.perform(post("/api/parishes").contentType(MediaType.APPLICATION_JSON).content(parishJson)).andReturn();
        Long parishId = ((Number) objectMapper.readValue(parishResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        CrudRequest.BranchUpsert branchReq = new CrudRequest.BranchUpsert("Scenario Branch", "123 Scenario St", stateId, muniId, parishId);
        MvcResult branchResult = mockMvc.perform(post("/api/branches").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(branchReq))).andReturn();
        Long branchId = ((Number) objectMapper.readValue(branchResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        CrudRequest.DepartmentUpsert deptReq = new CrudRequest.DepartmentUpsert("Scenario Dept", branchId);
        MvcResult deptResult = mockMvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(deptReq))).andReturn();
        Long deptId = ((Number) objectMapper.readValue(deptResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        CrudRequest.ItemUpsert itemReq = new CrudRequest.ItemUpsert("Scenario Item", 10, Item.UnitType.UND, "Obs", "{}", null, branchId, deptId);
        MvcResult itemResult = mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(itemReq))).andReturn();
        Long itemId = ((Number) objectMapper.readValue(itemResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        // 2. Register the Displacement (The "Zero Paperwork" action)
        CrudRequest.DisplacementUpsert borrowReq = new CrudRequest.DisplacementUpsert(
            null, 
            itemId,
            "Borrowing for afternoon repair",
            "Gabriel Ramos",
            null
        );

        mockMvc.perform(post("/api/displacements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(borrowReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.borrowerName").value("Gabriel Ramos"));

        // 3. Verify the item is now marked as displaced in the system
        mockMvc.perform(get("/api/displacements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.borrowerName == 'Gabriel Ramos')]").exists());
    }
}
