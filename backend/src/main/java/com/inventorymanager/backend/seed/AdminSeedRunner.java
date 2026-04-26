package com.inventorymanager.backend.seed;

import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.*;
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
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final StateRepository stateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final ParishRepository parishRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeedRunner(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            StateRepository stateRepository,
            MunicipalityRepository municipalityRepository,
            ParishRepository parishRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.stateRepository = stateRepository;
        this.municipalityRepository = municipalityRepository;
        this.parishRepository = parishRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed Permissions
        List<String> entities = List.of(
                "user", "role", "permission", "department", "category", "item", "state", "municipality", "parish", "item_request", "branch", "bag", "displacement"
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

        // 2. Seed Roles
        Role adminRole = roleRepository.findByName("admin").orElseGet(() -> {
            Role created = new Role();
            created.setName("admin");
            created.setDescription("Administrator role with full permissions");
            return roleRepository.save(created);
        });
        adminRole.setPermissions(new java.util.HashSet<>(allPermissions));
        adminRole = roleRepository.save(adminRole);
        final Role effectiveAdminRole = adminRole;

        // 3. Seed Default Location and Branch
        State defaultState = stateRepository.findByName("Default State").orElseGet(() -> {
            State s = new State();
            s.setName("Default State");
            return stateRepository.save(s);
        });
        Municipality defaultMunicipality = municipalityRepository.findAll().stream().filter(m -> m.getName().equals("Default Municipality")).findFirst().orElseGet(() -> {
            Municipality m = new Municipality();
            m.setName("Default Municipality");
            m.setState(defaultState);
            return municipalityRepository.save(m);
        });
        Parish defaultParish = parishRepository.findAll().stream().filter(p -> p.getName().equals("Default Parish")).findFirst().orElseGet(() -> {
            Parish p = new Parish();
            p.setName("Default Parish");
            p.setMunicipality(defaultMunicipality);
            return parishRepository.save(p);
        });

        Branch defaultBranch = branchRepository.findByName("Main Branch").orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Main Branch");
            b.setAddress("Main Street 123");
            b.setState(defaultState);
            b.setMunicipality(defaultMunicipality);
            b.setParish(defaultParish);
            return branchRepository.save(b);
        });

        // 4. Seed Default Departments
        Department inboundDept = departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Inbound") && d.getBranch().getId().equals(defaultBranch.getId()))
                .findFirst().orElseGet(() -> {
                    Department d = new Department();
                    d.setName("Inbound");
                    d.setBranch(defaultBranch);
                    return departmentRepository.save(d);
                });
        
        departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Storage") && d.getBranch().getId().equals(defaultBranch.getId()))
                .findFirst().orElseGet(() -> {
                    Department d = new Department();
                    d.setName("Storage");
                    d.setBranch(defaultBranch);
                    return departmentRepository.save(d);
                });

        // 5. Seed Admin User
        String adminUsername = System.getenv("ADMIN_USERNAME");
        if (adminUsername == null || adminUsername.isBlank()) {
            adminUsername = "admin";
        }

        String adminPassword = System.getenv("ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = "password";
        }

        final String finalAdminUsername = adminUsername;
        final String finalAdminPassword = adminPassword;

        userRepository.findByUsername(finalAdminUsername).ifPresentOrElse(user -> {
            if (user.getBranch() == null) {
                user.setBranch(defaultBranch);
                userRepository.save(user);
            }
        }, () -> {
            User admin = new User();
            admin.setUsername(finalAdminUsername);
            admin.setPasswordHash(passwordEncoder.encode(finalAdminPassword));
            admin.setRoles(new java.util.HashSet<>(java.util.Set.of(effectiveAdminRole)));
            admin.setBranch(defaultBranch);
            userRepository.save(admin);
        });

        // 6. Seed Operator Role
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
                "submit_item_request",
                "get_branch",
                "get_bag",
                "get_displacement",
                "create_displacement"
        );
        Set<Permission> operatorPermissions = allPermissions.stream()
                .filter(p -> operatorPermissionNames.contains(p.getName()))
                .collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new));
        operatorRole.setPermissions(operatorPermissions);
        roleRepository.save(operatorRole);
    }
}
