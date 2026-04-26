package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    java.util.Optional<Branch> findByName(String name);
}
