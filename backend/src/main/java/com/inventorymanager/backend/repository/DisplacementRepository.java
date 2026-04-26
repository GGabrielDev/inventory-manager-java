package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Displacement;
import com.inventorymanager.backend.domain.DisplacementStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisplacementRepository extends JpaRepository<Displacement, Long> {
    List<Displacement> findAllByStatus(DisplacementStatus status);
}
