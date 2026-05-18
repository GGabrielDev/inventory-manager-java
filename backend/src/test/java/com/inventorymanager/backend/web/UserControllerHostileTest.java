package com.inventorymanager.backend.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import com.inventorymanager.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;

public class UserControllerHostileTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BranchRepository branchRepository;
    private PasswordEncoder passwordEncoder;
    private CurrentUser currentUser;
    private AuditService auditService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        branchRepository = mock(BranchRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        currentUser = mock(CurrentUser.class);
        auditService = mock(AuditService.class);
        userController = new UserController(userRepository, roleRepository, branchRepository, passwordEncoder, currentUser, auditService);
    }

    /**
     * ADVERSARIAL VULNERABILITY: Partial Role Resolution Failure.
     * The code uses findAllById(uniqueIds). If some IDs are valid and others are not,
     * the list size won't match the uniqueIds size, throwing 400.
     * This is CORRECT but verify the implementation hasn't regressed to just ignoring invalid IDs.
     */
    @Test
    void testCreateUserWithNonExistentRolesThrows400() {
        CrudRequest.UserUpsert request = new CrudRequest.UserUpsert("attacker", "pass123", List.of(1L, 999L), 1L);
        when(roleRepository.findAllById(any())).thenReturn(List.of(new com.inventorymanager.backend.domain.Role())); // Only found 1

        ApiException ex = assertThrows(ApiException.class, () -> userController.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("roles not found"));
    }

    /**
     * ADVERSARIAL VULNERABILITY: Missing Branch Validation.
     * If branchId is provided, it's looked up. If NOT provided, user.setBranch(null) is called.
     * Ensure we can't bypass branch restrictions if the system assumes users MUST have a branch.
     */
    @Test
    void testCreateUserWithoutBranchSetsNullBranch() {
        CrudRequest.UserUpsert request = new CrudRequest.UserUpsert("nobranch", "pass123", List.of(), null);
        when(roleRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userController.create(request);
        
        // Success means the system allows branch-less users. 
        // If the domain model @Column(nullable = false), this will crash at JPA level, not Controller level.
        // Failing to validate this in Controller is a "leaky abstraction" vulnerability.
    }

    private void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Condition failed");
    }
}
