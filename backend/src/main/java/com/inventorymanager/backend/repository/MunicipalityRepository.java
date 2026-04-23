package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {}
