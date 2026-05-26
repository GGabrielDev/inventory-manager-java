package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Permission;
import com.inventorymanager.backend.repository.PermissionRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    private final PermissionRepository repository;
    private final RoleRepository roleRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public PermissionController(PermissionRepository repository, RoleRepository roleRepository, CurrentUser currentUser, AuditService auditService) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_permission')")
    public PageResponse<Permission> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_permission')")
    public Permission get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Permission not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_permission')")
    public Permission create(@Valid @RequestBody CrudRequest.PermissionUpsert request) {
        Permission entity = new Permission();
        entity.setName(request.name());
        entity.setDescription(request.description());
        Permission saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_permission')")
    public Permission update(@PathVariable Long id, @Valid @RequestBody CrudRequest.PermissionUpsert request) {
        Permission entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Permission not found"));
        entity.setName(request.name());
        entity.setDescription(request.description());
        Permission saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_permission')")
    public void delete(@PathVariable Long id) {
        Permission entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Permission not found"));
        if (roleRepository.existsByPermissions_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete Permission '" + entity.getName() + "' because it is assigned to one or more roles.");
        }
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }
}
