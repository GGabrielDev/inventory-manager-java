package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"branch", "roles"})
    Page<User> findAll(Pageable pageable);
}
