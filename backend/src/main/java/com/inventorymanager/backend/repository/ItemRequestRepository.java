package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.ItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {
    @Override
    @EntityGraph(attributePaths = {"requestedBy", "reviewedBy", "executedBy", "targetBranch", "entries"})
    Page<ItemRequest> findAll(Pageable pageable);

    boolean existsByTargetBranch_Id(Long branchId);
}
