package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.BagItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BagItemRepository extends JpaRepository<BagItem, Long> {
}
