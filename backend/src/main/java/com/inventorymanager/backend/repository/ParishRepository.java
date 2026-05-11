package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Parish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ParishRepository extends JpaRepository<Parish, Long>, JpaSpecificationExecutor<Parish> {
    @Override
    @EntityGraph(attributePaths = {"municipality"})
    Page<Parish> findAll(Specification<Parish> spec, Pageable pageable);
}
