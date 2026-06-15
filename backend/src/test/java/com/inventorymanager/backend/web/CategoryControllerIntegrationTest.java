package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.Category;
import com.inventorymanager.backend.repository.CategoryRepository;
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
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Category savedCategory;

    @BeforeEach
    void setUp() {
        Category c = new Category();
        c.setName("Test Category");
        savedCategory = categoryRepository.save(c);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_category"})
    void listReturnsPaginatedCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Test Category"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_category"})
    void getByIdReturnsCategory() throws Exception {
        mockMvc.perform(get("/api/categories/" + savedCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Category"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_category"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/categories/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_category"})
    void createCategorySucceeds() throws Exception {
        CrudRequest.NamedUpsert request = new CrudRequest.NamedUpsert("New Category");
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Category"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_category"})
    void updateCategorySucceeds() throws Exception {
        CrudRequest.NamedUpsert request = new CrudRequest.NamedUpsert("Updated Category");
        mockMvc.perform(put("/api/categories/" + savedCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Category"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_category"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.NamedUpsert request = new CrudRequest.NamedUpsert("Updated Category");
        mockMvc.perform(put("/api/categories/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_category"})
    void deleteCategoryWithNoDependentsSucceeds() throws Exception {
        mockMvc.perform(delete("/api/categories/" + savedCategory.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_category"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/categories/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isForbidden());
    }
}
