package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Bag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BagRepository extends JpaRepository<Bag, Long> {
    Optional<Bag> findByBarcode(String barcode);
}
