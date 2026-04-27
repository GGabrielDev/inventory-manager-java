package com.inventorymanager.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
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
class FunctionalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", authorities = {"create_branch", "get_branch", "create_department", "get_item", "create_state", "create_municipality", "create_parish"})
    void fullBranchAndItemWorkflow() throws Exception {
        // 1. Create a State (Dependency)
        String stateJson = objectMapper.writeValueAsString(Map.of("name", "Functional Test State"));
        MvcResult stateResult = mockMvc.perform(post("/api/states")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stateJson))
                .andExpect(status().isOk())
                .andReturn();
        Long stateId = ((Number) objectMapper.readValue(stateResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        // 2. Create Municipality
        String muniJson = objectMapper.writeValueAsString(new CrudRequest.MunicipalityUpsert("Functional Muni", stateId));
        MvcResult muniResult = mockMvc.perform(post("/api/municipalities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(muniJson))
                .andExpect(status().isOk())
                .andReturn();
        Long muniId = ((Number) objectMapper.readValue(muniResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        // 3. Create Parish
        String parishJson = objectMapper.writeValueAsString(new CrudRequest.ParishUpsert("Functional Parish", muniId));
        MvcResult parishResult = mockMvc.perform(post("/api/parishes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(parishJson))
                .andExpect(status().isOk())
                .andReturn();
        Long parishId = ((Number) objectMapper.readValue(parishResult.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        // 4. Create Branch
        CrudRequest.BranchUpsert branchReq = new CrudRequest.BranchUpsert("Functional Branch", "123 API Way", stateId, muniId, parishId);
        MvcResult branchResult = mockMvc.perform(post("/api/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(branchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Functional Branch"))
                .andReturn();
        
        // 5. Verify Branch appears in list
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Functional Branch')]").exists());
    }
}
