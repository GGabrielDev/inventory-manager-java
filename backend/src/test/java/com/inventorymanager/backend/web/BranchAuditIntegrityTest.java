package com.inventorymanager.backend.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.domain.Branch;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.ParishRepository;
import com.inventorymanager.backend.repository.StateRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BranchAuditIntegrityTest {

    private BranchController controller;
    private AuditService auditService;
    private BranchRepository branchRepository;
    private DepartmentRepository departmentRepository;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        branchRepository = Mockito.mock(BranchRepository.class);
        departmentRepository = Mockito.mock(DepartmentRepository.class);
        var stateRepository = Mockito.mock(StateRepository.class);
        var municipalityRepository = Mockito.mock(MunicipalityRepository.class);
        var parishRepository = Mockito.mock(ParishRepository.class);
        auditService = Mockito.mock(AuditService.class);
        currentUser = Mockito.mock(CurrentUser.class);
        Mockito.when(currentUser.id()).thenReturn(1L);

        // Mock lookups with valid hierarchy to satisfy invariants
        com.inventorymanager.backend.domain.State state = new com.inventorymanager.backend.domain.State();
        state.setId(1L);
        com.inventorymanager.backend.domain.Municipality municipality = new com.inventorymanager.backend.domain.Municipality();
        municipality.setId(1L);
        municipality.setState(state);
        com.inventorymanager.backend.domain.Parish parish = new com.inventorymanager.backend.domain.Parish();
        parish.setId(1L);
        parish.setMunicipality(municipality);

        Mockito.when(stateRepository.findById(any())).thenReturn(Optional.of(state));
        Mockito.when(municipalityRepository.findById(any())).thenReturn(Optional.of(municipality));
        Mockito.when(parishRepository.findById(any())).thenReturn(Optional.of(parish));

        controller = new BranchController(
                branchRepository,
                stateRepository,
                municipalityRepository,
                parishRepository,
                departmentRepository,
                Mockito.mock(com.inventorymanager.backend.repository.ItemRepository.class),
                Mockito.mock(com.inventorymanager.backend.repository.UserRepository.class),
                Mockito.mock(com.inventorymanager.backend.repository.BagRepository.class),
                Mockito.mock(com.inventorymanager.backend.repository.ItemRequestRepository.class),
                currentUser,
                auditService
        );
    }

    /**
     * ADVERSARIAL TEST: Integrity Violation - Missing Branch Update Audit.
     * The BranchController.update adds @Transactional but fails to commit the audit log for the branch itself.
     */
    @Test
    void updateFailsToAuditBranch() {
        Long branchId = 1L;
        Branch branch = new Branch();
        branch.setId(branchId);
        Mockito.when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        Mockito.when(branchRepository.save(any())).thenReturn(branch);

        CrudRequest.BranchUpsert request = new CrudRequest.BranchUpsert("New Name", "Address", 1L, 1L, 1L);
        controller.update(branchId, request);

        // This SHOULD fail if the code doesn't call commitUpdate for the branch
        verify(auditService, times(1)).commitUpdate(eq(1L), any(Branch.class));
    }
}
