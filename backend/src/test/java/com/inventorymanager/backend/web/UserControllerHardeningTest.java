package com.inventorymanager.backend.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.domain.User;
import com.inventorymanager.backend.repository.UserRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import com.inventorymanager.backend.audit.AuditService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class UserControllerHardeningTest {

    @Test
    public void testCreateUserWithInvalidRoleIdsThrowsException() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        org.springframework.security.crypto.password.PasswordEncoder encoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        UserController controller = new UserController(null, roleRepository, null, encoder, null, null);
        
        List<Long> requestedRoleIds = List.of(1L, 999L);
        Role role1 = new Role(); role1.setId(1L);
        when(roleRepository.findAllById(any())).thenReturn(List.of(role1));
        when(encoder.encode(any())).thenReturn("hash");
        
        CrudRequest.UserUpsert req = new CrudRequest.UserUpsert("user", "pass", requestedRoleIds, null);
        
        ApiException ex = assertThrows(ApiException.class, () -> controller.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("One or more roles not found", ex.getMessage());
    }

    @Test
    public void testCreateUserWithDuplicateValidRoleIdsPasses() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        org.springframework.security.crypto.password.PasswordEncoder encoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        com.inventorymanager.backend.auth.CurrentUser currentUser = mock(com.inventorymanager.backend.auth.CurrentUser.class);
        when(currentUser.id()).thenReturn(1L);
        
        UserController controller = new UserController(userRepository, roleRepository, null, encoder, currentUser, mock(AuditService.class));
        
        List<Long> roleIds = List.of(1L, 1L);
        Role role1 = new Role(); role1.setId(1L);
        when(roleRepository.findAllById(any())).thenReturn(List.of(role1));
        when(encoder.encode(any())).thenReturn("hash");
        
        User saved = new User(); saved.setId(1L); saved.setUsername("user");
        when(userRepository.save(any())).thenReturn(saved);

        CrudRequest.UserUpsert req = new CrudRequest.UserUpsert("user", "pass", roleIds, null);
        controller.create(req);
    }
}
