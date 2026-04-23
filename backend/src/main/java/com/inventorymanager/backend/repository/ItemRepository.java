package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {}
