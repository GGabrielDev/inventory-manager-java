package com.inventorymanager.backend.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(AuditingConfig.class)
class BranchRepositoryIntegrationTest {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private ParishRepository parishRepository;

    @Test
    void canSaveAndRetrieveBranch() {
        // Setup locations
        State state = new State();
        state.setName("Test State");
        state = stateRepository.save(state);

        Municipality municipality = new Municipality();
        municipality.setName("Test Municipality");
        municipality.setState(state);
        municipality = municipalityRepository.save(municipality);

        Parish parish = new Parish();
        parish.setName("Test Parish");
        parish.setMunicipality(municipality);
        parish = parishRepository.save(parish);

        // Save branch
        Branch branch = new Branch();
        branch.setName("Test Branch");
        branch.setAddress("123 Test St");
        branch.setState(state);
        branch.setMunicipality(municipality);
        branch.setParish(parish);

        Branch saved = branchRepository.save(branch);
        assertNotNull(saved.getId());

        // Retrieve
        Branch found = branchRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Test Branch", found.getName());
        assertEquals("Test State", found.getState().getName());
    }
}
