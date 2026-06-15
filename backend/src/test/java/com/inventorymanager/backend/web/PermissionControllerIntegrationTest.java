package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.Permission;
import com.inventorymanager.backend.repository.PermissionRepository;
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
class PermissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Permission savedPermission;

    @BeforeEach
    void setUp() {
        // Note: AdminSeedRunner creates ~59 permissions before this runs.
        Permission p = new Permission();
        p.setName("test_custom_permission");
        p.setDescription("Custom test permission");
        savedPermission = permissionRepository.save(p);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_permission"})
    void listReturnsPaginatedPermissions() throws Exception {
        mockMvc.perform(get("/api/permissions?pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNumber())
                .andExpect(jsonPath("$.data[?(@.name == 'test_custom_permission')].description").value("Custom test permission"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_permission"})
    void getByIdReturnsPermission() throws Exception {
        mockMvc.perform(get("/api/permissions/" + savedPermission.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test_custom_permission"))
                .andExpect(jsonPath("$.description").value("Custom test permission"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_permission"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/permissions/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_permission"})
    void createPermissionSucceeds() throws Exception {
        CrudRequest.PermissionUpsert request = new CrudRequest.PermissionUpsert("new_perm", "New permission description");
        mockMvc.perform(post("/api/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new_perm"))
                .andExpect(jsonPath("$.description").value("New permission description"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_permission"})
    void updatePermissionSucceeds() throws Exception {
        CrudRequest.PermissionUpsert request = new CrudRequest.PermissionUpsert("updated_perm", "Updated description");
        mockMvc.perform(put("/api/permissions/" + savedPermission.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated_perm"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_permission"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.PermissionUpsert request = new CrudRequest.PermissionUpsert("updated_perm", "Updated description");
        mockMvc.perform(put("/api/permissions/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_permission"})
    void deletePermissionWithNoDependentsSucceeds() throws Exception {
        mockMvc.perform(delete("/api/permissions/" + savedPermission.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_permission"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/permissions/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isForbidden());
    }
}
