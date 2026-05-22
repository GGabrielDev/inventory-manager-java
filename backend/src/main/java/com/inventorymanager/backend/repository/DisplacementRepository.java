package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Displacement;
import com.inventorymanager.backend.domain.DisplacementStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DisplacementRepository extends JpaRepository<Displacement, Long> {
    List<Displacement> findAllByStatus(DisplacementStatus status);

    @Query("SELECT d FROM Displacement d WHERE d.bag.id = :bagId AND d.status = 'ACTIVE'")
    List<Displacement> findActiveByBag(Long bagId);

    @Override
    @EntityGraph(attributePaths = {"bag", "item"})
    Page<Displacement> findAll(Pageable pageable);

    boolean existsByItem_IdAndStatus(Long itemId, DisplacementStatus status);
    boolean existsByBag_IdAndStatus(Long bagId, DisplacementStatus status);
}
