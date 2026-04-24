package com.inventorymanager.backend.seed;

import com.inventorymanager.backend.domain.Permission;
import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.domain.User;
import com.inventorymanager.backend.repository.PermissionRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import com.inventorymanager.backend.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminSeedRunner implements CommandLineRunner {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeedRunner(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<String> entities = List.of(
                "user", "role", "permission", "department", "category", "item", "state", "municipality", "parish", "item_request"
        );
        List<String> actions = List.of("create", "get", "edit", "delete");

        List<Permission> allPermissions = new ArrayList<>();
        for (String entity : entities) {
            for (String action : actions) {
                String permissionName = action + "_" + entity;
                Permission permission = permissionRepository.findByName(permissionName).orElseGet(() -> {
                    Permission created = new Permission();
                    created.setName(permissionName);
                    created.setDescription("Allows a user to " + action + " a " + entity);
                    return permissionRepository.save(created);
                });
                allPermissions.add(permission);
            }
        }

        List<String> workflowPermissions = List.of(
                "submit_item_request",
                "review_item_request",
                "execute_item_request"
        );
        for (String permissionName : workflowPermissions) {
            Permission permission = permissionRepository.findByName(permissionName).orElseGet(() -> {
                Permission created = new Permission();
                created.setName(permissionName);
                created.setDescription("Allows a user to " + permissionName.replace('_', ' '));
                return permissionRepository.save(created);
            });
            allPermissions.add(permission);
        }

        Role adminRole = roleRepository.findByName("admin").orElseGet(() -> {
            Role created = new Role();
            created.setName("admin");
            created.setDescription("Administrator role with full permissions");
            return roleRepository.save(created);
        });
        adminRole.setPermissions(Set.copyOf(allPermissions));
        adminRole = roleRepository.save(adminRole);
        final Role effectiveAdminRole = adminRole;

        userRepository.findByUsername("admin").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin"));
            admin.setRoles(Set.of(effectiveAdminRole));
            return userRepository.save(admin);
        });

        Role operatorRole = roleRepository.findByName("operator").orElseGet(() -> {
            Role created = new Role();
            created.setName("operator");
            created.setDescription("Operator role for request-based inventory operations");
            return roleRepository.save(created);
        });

        Set<String> operatorPermissionNames = Set.of(
                "get_item",
                "get_category",
                "get_department",
                "get_state",
                "get_municipality",
                "get_parish",
                "get_item_request",
                "create_item_request",
                "edit_item_request",
                "submit_item_request"
        );
        Set<Permission> operatorPermissions = allPermissions.stream()
                .filter(p -> operatorPermissionNames.contains(p.getName()))
                .collect(java.util.stream.Collectors.toSet());
        operatorRole.setPermissions(operatorPermissions);
        roleRepository.save(operatorRole);
    }
}
