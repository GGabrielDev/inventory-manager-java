package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.State;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<State, Long> {}
