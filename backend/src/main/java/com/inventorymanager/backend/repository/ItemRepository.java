package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    @Override
    @EntityGraph(attributePaths = {"category", "branch", "department"})
    Page<Item> findAll(Specification<Item> spec, Pageable pageable);

    boolean existsByBranch_Id(Long branchId);
    boolean existsByDepartment_Id(Long departmentId);
    boolean existsByCategory_Id(Long categoryId);
}
