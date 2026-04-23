package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {}
