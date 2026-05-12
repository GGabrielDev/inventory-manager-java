package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Bag;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BagRepository extends JpaRepository<Bag, Long> {
    Optional<Bag> findByBarcode(String barcode);

    @Override
    @EntityGraph(attributePaths = {"branch", "assignedDepartment"})
    Page<Bag> findAll(Pageable pageable);
}
