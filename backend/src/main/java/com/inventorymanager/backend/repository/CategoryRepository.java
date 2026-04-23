package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
