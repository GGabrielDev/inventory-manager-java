package com.inventorymanager.backend.repository;

import com.inventorymanager.backend.domain.Municipality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MunicipalityRepository extends JpaRepository<Municipality, Long>, JpaSpecificationExecutor<Municipality> {
    @Override
    @EntityGraph(attributePaths = {"state"})
    Page<Municipality> findAll(Specification<Municipality> spec, Pageable pageable);

    boolean existsByState_Id(Long stateId);
}
