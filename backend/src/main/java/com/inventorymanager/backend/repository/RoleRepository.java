package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @Override
    @EntityGraph(attributePaths = {"permissions"})
    Page<Role> findAll(Pageable pageable);
}
