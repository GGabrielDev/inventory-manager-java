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
class ItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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

    private Item savedItem;
    private Category savedCategory;
    private Branch savedBranch;
    private Department savedDepartment;

    @BeforeEach
    void setUp() {
        State state = stateRepository.save(createState("Item Test State"));
        Municipality muni = municipalityRepository.save(createMunicipality("Item Test Muni", state));
        Parish parish = parishRepository.save(createParish("Item Test Parish", muni));
        savedBranch = branchRepository.save(createBranch("Item Test Branch", state, muni, parish));

        savedDepartment = new Department();
        savedDepartment.setName("Item Test Dept");
        savedDepartment.setBranch(savedBranch);
        savedDepartment = departmentRepository.save(savedDepartment);

        savedCategory = new Category();
        savedCategory.setName("Item Test Category");
        savedCategory = categoryRepository.save(savedCategory);

        Item item = new Item();
        item.setName("Test Item");
        item.setQuantity(10);
        item.setUnit(Item.UnitType.UND);
        item.setBranch(savedBranch);
        item.setDepartment(savedDepartment);
        item.setCategory(savedCategory);
        savedItem = itemRepository.save(item);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_item"})
    void listReturnsPaginatedItems() throws Exception {
        mockMvc.perform(get("/api/items?pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Test Item')].quantity").value(10));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_item"})
    void listByCategoryReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/items?categoryId=" + savedCategory.getId() + "&pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Item"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_item"})
    void listByBranchReturnsFiltered() throws Exception {
        mockMvc.perform(get("/api/items?branchId=" + savedBranch.getId() + "&pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Item"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_item"})
    void getByIdReturnsItem() throws Exception {
        mockMvc.perform(get("/api/items/" + savedItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Item"))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_item"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/items/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_item"})
    void createItemSucceeds() throws Exception {
        CrudRequest.ItemUpsert request = new CrudRequest.ItemUpsert(
                "New Item", 5, Item.UnitType.KG,
                "Test observations", null,
                savedCategory.getId(), savedBranch.getId(), savedDepartment.getId());
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Item"))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_item"})
    void createItemWithInvalidBranchReturns400() throws Exception {
        CrudRequest.ItemUpsert request = new CrudRequest.ItemUpsert(
                "Bad Item", 1, Item.UnitType.UND,
                null, null,
                savedCategory.getId(), 99999L, savedDepartment.getId());
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_item"})
    void updateItemSucceeds() throws Exception {
        CrudRequest.ItemUpsert request = new CrudRequest.ItemUpsert(
                "Updated Item", 20, Item.UnitType.L,
                "Updated obs", null,
                savedCategory.getId(), savedBranch.getId(), savedDepartment.getId());
        mockMvc.perform(put("/api/items/" + savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Item"))
                .andExpect(jsonPath("$.quantity").value(20));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_item"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.ItemUpsert request = new CrudRequest.ItemUpsert(
                "Updated Item", 20, Item.UnitType.UND,
                null, null,
                savedCategory.getId(), savedBranch.getId(), savedDepartment.getId());
        mockMvc.perform(put("/api/items/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_item"})
    void deleteItemWithNoDependentsSucceeds() throws Exception {
        mockMvc.perform(delete("/api/items/" + savedItem.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_item"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/items/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/items"))
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
