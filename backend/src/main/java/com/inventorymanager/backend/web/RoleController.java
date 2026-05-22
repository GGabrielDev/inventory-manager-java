package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Permission;
import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.repository.PermissionRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import com.inventorymanager.backend.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleRepository repository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public RoleController(
            RoleRepository repository,
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_role')")
    public PageResponse<Role> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_role')")
    public Role get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_role')")
    public Role create(@Valid @RequestBody CrudRequest.RoleUpsert request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissions(fetchPermissions(request.permissionIds()));
        Role saved = repository.save(role);
        auditService.commitCreate(currentUser.id(), saved);
        saved.getPermissions().forEach(permission ->
                auditService.commitLink(currentUser.id(), "role_permissions", saved.getId(), permission.getId()));
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_role')")
    public Role update(@PathVariable Long id, @Valid @RequestBody CrudRequest.RoleUpsert request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        Role role = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role not found"));
        Set<Long> previous = role.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissions(fetchPermissions(request.permissionIds()));
        Role saved = repository.save(role);
        auditService.commitUpdate(currentUser.id(), saved);
        Set<Long> current = saved.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
        for (Long p : current) {
            if (!previous.contains(p)) {
                auditService.commitLink(currentUser.id(), "role_permissions", saved.getId(), p);
            }
        }
        for (Long p : previous) {
            if (!current.contains(p)) {
                auditService.commitUnlink(currentUser.id(), "role_permissions", saved.getId(), p);
            }
        }
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_role')")
    public void delete(@PathVariable Long id) {
        Role role = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role not found"));
        if (userRepository.existsByRoles_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete Role '" + role.getName() + "' because it is assigned to one or more users.");
        }
        repository.delete(role);
        auditService.commitDelete(currentUser.id(), role);
    }

    private Set<Permission> fetchPermissions(java.util.List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new java.util.HashSet<>();
        }
        java.util.Set<Long> uniqueIds = new java.util.HashSet<>(permissionIds);
        java.util.List<Permission> permissions = permissionRepository.findAllById(uniqueIds);
        if (permissions.size() != uniqueIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more permissions not found");
        }
        return new java.util.HashSet<>(permissions);
    }
}
