package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.domain.User;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import com.inventorymanager.backend.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public UserController(
            UserRepository repository,
            RoleRepository roleRepository,
            BranchRepository branchRepository,
            PasswordEncoder passwordEncoder,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_user')")
    public PageResponse<User> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_user')")
    public User get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_user')")
    public User create(@Valid @RequestBody CrudRequest.UserUpsert request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required for new users");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(fetchRoles(request.roleIds()));
        
        if (request.branchId() != null) {
            user.setBranch(branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Branch not found")));
        }
        
        User saved = repository.save(user);
        auditService.commitCreate(currentUser.id(), saved);
        saved.getRoles().forEach(role ->
                auditService.commitLink(currentUser.id(), "user_roles", saved.getId(), role.getId()));
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_user')")
    public User update(@PathVariable Long id, @Valid @RequestBody CrudRequest.UserUpsert request) {
        User user = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Set<Long> previous = user.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        user.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setRoles(fetchRoles(request.roleIds()));
        
        if (request.branchId() != null) {
            user.setBranch(branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Branch not found")));
        } else {
            user.setBranch(null);
        }
        
        User saved = repository.save(user);
        auditService.commitUpdate(currentUser.id(), saved);
        Set<Long> current = saved.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        for (Long p : current) {
            if (!previous.contains(p)) {
                auditService.commitLink(currentUser.id(), "user_roles", saved.getId(), p);
            }
        }
        for (Long p : previous) {
            if (!current.contains(p)) {
                auditService.commitUnlink(currentUser.id(), "user_roles", saved.getId(), p);
            }
        }
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_user')")
    public void delete(@PathVariable Long id) {
        User user = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        repository.delete(user);
        auditService.commitDelete(currentUser.id(), user);
    }

    private Set<Role> fetchRoles(java.util.List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new java.util.HashSet<>();
        }
        java.util.Set<Long> uniqueIds = new java.util.HashSet<>(roleIds);
        java.util.List<Role> roles = roleRepository.findAllById(uniqueIds);
        if (roles.size() != uniqueIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more roles not found");
        }
        return new java.util.HashSet<>(roles);
    }
}
