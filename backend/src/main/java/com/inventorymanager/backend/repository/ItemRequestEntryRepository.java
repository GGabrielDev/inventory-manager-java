package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.ItemRequestEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRequestEntryRepository extends JpaRepository<ItemRequestEntry, Long> {}
