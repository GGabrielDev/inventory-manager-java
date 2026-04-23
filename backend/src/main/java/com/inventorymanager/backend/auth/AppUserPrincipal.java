package com.inventorymanager.backend.auth;

import com.inventorymanager.backend.domain.Permission;
import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.domain.User;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserPrincipal implements UserDetails {
    private final User user;

    public AppUserPrincipal(User user) {
        this.user = user;
    }

    public Long id() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> permissionNames = user.getRoles().stream()
                .map(Role::getPermissions)
                .flatMap(Set::stream)
                .map(Permission::getName)
                .collect(Collectors.toSet());
        return permissionNames.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }
}
