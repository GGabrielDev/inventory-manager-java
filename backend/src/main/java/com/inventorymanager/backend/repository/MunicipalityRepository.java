package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MunicipalityRepository extends JpaRepository<Municipality, Long>, JpaSpecificationExecutor<Municipality> {}
