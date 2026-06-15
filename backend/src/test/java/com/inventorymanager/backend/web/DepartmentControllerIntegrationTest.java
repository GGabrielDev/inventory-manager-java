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
class DepartmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private ParishRepository parishRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Department savedDepartment;
    private Branch savedBranch;

    @BeforeEach
    void setUp() {
        State state = stateRepository.save(createState("Dept Test State"));
        Municipality muni = municipalityRepository.save(createMunicipality("Dept Test Muni", state));
        Parish parish = parishRepository.save(createParish("Dept Test Parish", muni));
        savedBranch = branchRepository.save(createBranch("Dept Test Branch", state, muni, parish));

        Department d = new Department();
        d.setName("Test Department");
        d.setBranch(savedBranch);
        savedDepartment = departmentRepository.save(d);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_department"})
    void listReturnsPaginatedDepartments() throws Exception {
        mockMvc.perform(get("/api/departments?pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNumber())
                .andExpect(jsonPath("$.data[?(@.name == 'Test Department')].id").exists());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_department"})
    void listByBranchReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/departments?branchId=" + savedBranch.getId() + "&pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Department"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_department"})
    void getByIdReturnsDepartment() throws Exception {
        mockMvc.perform(get("/api/departments/" + savedDepartment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Department"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_department"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/departments/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_department"})
    void createDepartmentSucceeds() throws Exception {
        CrudRequest.DepartmentUpsert request = new CrudRequest.DepartmentUpsert("New Department", savedBranch.getId());
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Department"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_department"})
    void createDepartmentWithInvalidBranchReturns400() throws Exception {
        CrudRequest.DepartmentUpsert request = new CrudRequest.DepartmentUpsert("Bad Dept", 99999L);
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_department"})
    void updateDepartmentSucceeds() throws Exception {
        CrudRequest.DepartmentUpsert request = new CrudRequest.DepartmentUpsert("Updated Department", savedBranch.getId());
        mockMvc.perform(put("/api/departments/" + savedDepartment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Department"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_department"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.DepartmentUpsert request = new CrudRequest.DepartmentUpsert("Updated Dept", savedBranch.getId());
        mockMvc.perform(put("/api/departments/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_department"})
    void deleteDepartmentWithNoDependentsSucceeds() throws Exception {
        mockMvc.perform(delete("/api/departments/" + savedDepartment.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_department"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/departments/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/departments"))
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

    private static Branch createBranch(String name, State state, Municipality muni, Parish parish) {
        Branch b = new Branch();
        b.setName(name);
        b.setAddress("123 Test St");
        b.setState(state);
        b.setMunicipality(muni);
        b.setParish(parish);
        return b;
    }
}
