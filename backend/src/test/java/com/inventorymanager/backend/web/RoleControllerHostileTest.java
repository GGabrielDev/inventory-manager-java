package com.inventorymanager.backend.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.repository.PermissionRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.List;

public class RoleControllerHostileTest {

    private RoleRepository repository;
    private PermissionRepository permissionRepository;
    private CurrentUser currentUser;
    private AuditService auditService;
    private RoleController controller;

    @BeforeEach
    void setUp() {
        repository = mock(RoleRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        currentUser = mock(CurrentUser.class);
        auditService = mock(AuditService.class);
        controller = new RoleController(repository, permissionRepository, mock(com.inventorymanager.backend.repository.UserRepository.class), currentUser, auditService);
    }

    @Test
    void testCreateRoleWithPartialPermissionsThrows400() {
        CrudRequest.RoleUpsert request = new CrudRequest.RoleUpsert("role", "desc", List.of(1L, 2L));
        when(permissionRepository.findAllById(any())).thenReturn(List.of(new com.inventorymanager.backend.domain.Permission()));

        ApiException ex = assertThrows(ApiException.class, () -> controller.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("One or more permissions not found", ex.getMessage());
    }

    @Test
    void testCreateRoleWithNullBodyThrows400() {
        ApiException ex = assertThrows(ApiException.class, () -> controller.create(null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Request body is required", ex.getMessage());
    }
}
