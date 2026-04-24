package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.ItemRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {}
