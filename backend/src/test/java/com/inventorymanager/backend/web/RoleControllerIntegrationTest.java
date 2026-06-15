package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.Permission;
import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.repository.PermissionRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import java.util.List;
import java.util.Set;
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
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Permission savedPermission;

    @BeforeEach
    void setUp() {
        Permission p = new Permission();
        p.setName("test_role_permission");
        p.setDescription("Permission for role test");
        savedPermission = permissionRepository.save(p);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_role"})
    void listRolesReturnsData() throws Exception {
        Role role = new Role();
        role.setName("Test Role");
        role.setDescription("Test role description");
        role.setPermissions(Set.of(savedPermission));
        roleRepository.save(role);

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Test Role')].description").value("Test role description"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_role"})
    void getByIdReturnsRole() throws Exception {
        Role role = new Role();
        role.setName("Get Test Role");
        role.setDescription("Get test description");
        role.setPermissions(Set.of(savedPermission));
        Role saved = roleRepository.save(role);

        mockMvc.perform(get("/api/roles/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Get Test Role"))
                .andExpect(jsonPath("$.description").value("Get test description"))
                .andExpect(jsonPath("$.permissions[0].name").value("test_role_permission"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_role"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/roles/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_role"})
    void createRoleSucceeds() throws Exception {
        CrudRequest.RoleUpsert request = new CrudRequest.RoleUpsert(
                "New Role", "New role description", List.of(savedPermission.getId()));

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Role"))
                .andExpect(jsonPath("$.description").value("New role description"))
                .andExpect(jsonPath("$.permissions[0].name").value("test_role_permission"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_role"})
    void createRoleWithInvalidPermissionReturns400() throws Exception {
        CrudRequest.RoleUpsert request = new CrudRequest.RoleUpsert(
                "Bad Role", "Bad role description", List.of(99999L));

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_role"})
    void updateRoleSucceeds() throws Exception {
        Role role = new Role();
        role.setName("Old Role");
        role.setDescription("Old description");
        role.setPermissions(Set.of(savedPermission));
        Role saved = roleRepository.save(role);

        Permission newPerm = new Permission();
        newPerm.setName("update_role_perm");
        newPerm.setDescription("For update test");
        Permission savedNewPerm = permissionRepository.save(newPerm);

        CrudRequest.RoleUpsert request = new CrudRequest.RoleUpsert(
                "Updated Role", "Updated description", List.of(savedNewPerm.getId()));

        mockMvc.perform(put("/api/roles/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Role"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.permissions[0].name").value("update_role_perm"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_role"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.RoleUpsert request = new CrudRequest.RoleUpsert(
                "Ghost", "Ghost description", List.of());

        mockMvc.perform(put("/api/roles/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_role"})
    void deleteRoleSucceeds() throws Exception {
        Role role = new Role();
        role.setName("Delete Me");
        role.setDescription("Role to delete");
        role.setPermissions(Set.of(savedPermission));
        Role saved = roleRepository.save(role);

        mockMvc.perform(delete("/api/roles/" + saved.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_role"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/roles/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isForbidden());
    }
}
