package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private ParishRepository parishRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Branch savedBranch;
    private Role savedRole;

    @BeforeEach
    void setUp() {
        State state = new State();
        state.setName("User Test State");
        state = stateRepository.save(state);

        Municipality muni = new Municipality();
        muni.setName("User Test Muni");
        muni.setState(state);
        muni = municipalityRepository.save(muni);

        Parish parish = new Parish();
        parish.setName("User Test Parish");
        parish.setMunicipality(muni);
        parish = parishRepository.save(parish);

        savedBranch = new Branch();
        savedBranch.setName("User Test Branch");
        savedBranch.setAddress("456 User St");
        savedBranch.setState(state);
        savedBranch.setMunicipality(muni);
        savedBranch.setParish(parish);
        savedBranch = branchRepository.save(savedBranch);

        Permission perm = new Permission();
        perm.setName("user_test_perm");
        perm.setDescription("Permission for user test");
        perm = permissionRepository.save(perm);

        savedRole = new Role();
        savedRole.setName("User Test Role");
        savedRole.setDescription("Role for user test");
        savedRole.setPermissions(Set.of(perm));
        savedRole = roleRepository.save(savedRole);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_user"})
    void listUsersReturnsData() throws Exception {
        User user = new User();
        user.setUsername("listuser");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRoles(Set.of(savedRole));
        user.setBranch(savedBranch);
        userRepository.save(user);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.username == 'listuser')]").exists());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_user"})
    void getByIdReturnsUser() throws Exception {
        User user = new User();
        user.setUsername("getuser");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRoles(Set.of(savedRole));
        user.setBranch(savedBranch);
        User saved = userRepository.save(user);

        mockMvc.perform(get("/api/users/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("getuser"))
                .andExpect(jsonPath("$.roles[0].name").value("User Test Role"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_user"})
    void getByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_user"})
    void createUserSucceeds() throws Exception {
        CrudRequest.UserUpsert request = new CrudRequest.UserUpsert(
                "newuser", "securepass", List.of(savedRole.getId()), savedBranch.getId());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.roles[0].name").value("User Test Role"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_user"})
    void createUserWithInvalidRoleReturns400() throws Exception {
        CrudRequest.UserUpsert request = new CrudRequest.UserUpsert(
                "baduser", "pass", List.of(99999L), savedBranch.getId());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"create_user"})
    void createUserWithoutPasswordReturns400() throws Exception {
        CrudRequest.UserUpsert request = new CrudRequest.UserUpsert(
                "nopassword", null, List.of(savedRole.getId()), savedBranch.getId());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_user"})
    void updateUserSucceeds() throws Exception {
        User user = new User();
        user.setUsername("olduser");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRoles(Set.of(savedRole));
        user.setBranch(savedBranch);
        User saved = userRepository.save(user);

        CrudRequest.UserUpsert request = new CrudRequest.UserUpsert(
                "updateduser", "newpass", List.of(savedRole.getId()), savedBranch.getId());

        mockMvc.perform(put("/api/users/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"edit_user"})
    void updateInvalidIdReturns404() throws Exception {
        CrudRequest.UserUpsert request = new CrudRequest.UserUpsert(
                "ghost", "pass", List.of(), savedBranch.getId());

        mockMvc.perform(put("/api/users/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_user"})
    void deleteUserSucceeds() throws Exception {
        User user = new User();
        user.setUsername("deleteme");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRoles(Set.of(savedRole));
        user.setBranch(savedBranch);
        User saved = userRepository.save(user);

        mockMvc.perform(delete("/api/users/" + saved.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"delete_user"})
    void deleteInvalidIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anonymous", authorities = {})
    void unauthenticatedRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }
}
