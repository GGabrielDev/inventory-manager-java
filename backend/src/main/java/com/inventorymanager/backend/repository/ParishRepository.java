package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Parish;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ParishRepository extends JpaRepository<Parish, Long>, JpaSpecificationExecutor<Parish> {}
