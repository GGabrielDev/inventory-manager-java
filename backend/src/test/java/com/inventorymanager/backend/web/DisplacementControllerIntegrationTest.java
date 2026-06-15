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

import java.time.OffsetDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DisplacementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DisplacementRepository displacementRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BagRepository bagRepository;

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

    private Displacement savedDisplacement;
    private Item savedItem;
    private Branch savedBranch;
    private Department savedDepartment;

    @BeforeEach
    void setUp() {
        State state = stateRepository.save(createState("Disp Test State"));
        Municipality muni = municipalityRepository.save(createMunicipality("Disp Test Muni", state));
        Parish parish = parishRepository.save(createParish("Disp Test Parish", muni));
        savedBranch = branchRepository.save(createBranch("Disp Test Branch", state, muni, parish));

        savedDepartment = new Department();
        savedDepartment.setName("Disp Test Dept");
        savedDepartment.setBranch(savedBranch);
        savedDepartment = departmentRepository.save(savedDepartment);

        savedItem = new Item();
        savedItem.setName("Disp Test Item");
        savedItem.setQuantity(10);
        savedItem.setUnit(Item.UnitType.UND);
        savedItem.setBranch(savedBranch);
        savedItem.setDepartment(savedDepartment);
        savedItem = itemRepository.save(savedItem);

        Displacement d = new Displacement();
        d.setItem(savedItem);
        d.setReason("Test reason");
        d.setBorrowerName("John Doe");
        d.setExpectedReturnDate(OffsetDateTime.now().plusDays(7));
        d.setStatus(DisplacementStatus.RESOLVED); // RESOLVED so we can delete it
        savedDisplacement = displacementRepository.save(d);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_displacement"})
    void listReturnsPaginatedDisplacements() throws Exception {
        mockMvc.perform(get("/api/displacements?pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.reason == 'Test reason')].borrowerName").value("John Doe"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_displacement"})
    void getByIdReturnsDisplacement() throws Exception {
        mockMvc.perform(get("/api/displacements/" + savedDisplacement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Test reason"))
                .andExpect(jsonPath("$.borrowerName").value("John Doe"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_displacement"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/displacements/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_displacement"})
    void createDisplacementSucceeds() throws Exception {
        OffsetDateTime returnDate = OffsetDateTime.now().plusDays(14);
        CrudRequest.DisplacementUpsert request = new CrudRequest.DisplacementUpsert(
                null, savedItem.getId(), "Borrowing item", "Jane Doe", returnDate);
        mockMvc.perform(post("/api/displacements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Borrowing item"))
                .andExpect(jsonPath("$.borrowerName").value("Jane Doe"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_displacement"})
    void createDisplacementWithInvalidItemReturns400() throws Exception {
        CrudRequest.DisplacementUpsert request = new CrudRequest.DisplacementUpsert(
                null, 99999L, "Bad displacement", "John", OffsetDateTime.now());
        mockMvc.perform(post("/api/displacements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_displacement"})
    void updateDisplacementSucceeds() throws Exception {
        CrudRequest.DisplacementUpsert request = new CrudRequest.DisplacementUpsert(
                null, savedItem.getId(), "Updated reason", "Jane Updated", OffsetDateTime.now().plusDays(30));
        mockMvc.perform(put("/api/displacements/" + savedDisplacement.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Updated reason"))
                .andExpect(jsonPath("$.borrowerName").value("Jane Updated"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_displacement"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.DisplacementUpsert request = new CrudRequest.DisplacementUpsert(
                null, savedItem.getId(), "Updated", "Jane", OffsetDateTime.now());
        mockMvc.perform(put("/api/displacements/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_displacement"})
    void resolveDisplacementSucceeds() throws Exception {
        // Create an ACTIVE displacement first
        Displacement active = new Displacement();
        active.setItem(savedItem);
        active.setReason("To be resolved");
        active.setBorrowerName("Active Borrower");
        active.setStatus(DisplacementStatus.ACTIVE);
        active = displacementRepository.save(active);

        mockMvc.perform(post("/api/displacements/" + active.getId() + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_displacement"})
    void deleteResolvedDisplacementSucceeds() throws Exception {
        mockMvc.perform(delete("/api/displacements/" + savedDisplacement.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_displacement"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/displacements/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/displacements"))
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
