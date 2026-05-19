package com.inventorymanager.backend.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.domain.Branch;
import com.inventorymanager.backend.domain.Department;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import com.inventorymanager.backend.repository.DisplacementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.Optional;

public class BagControllerHostileValidationTest {

    private BagController controller;
    private BranchRepository branchRepo;
    private DepartmentRepository deptRepo;
    private BagRepository bagRepo;

    @BeforeEach
    void setUp() {
        branchRepo = mock(BranchRepository.class);
        deptRepo = mock(DepartmentRepository.class);
        bagRepo = mock(BagRepository.class);
        controller = new BagController(
            bagRepo, branchRepo, deptRepo, 
            mock(ItemRepository.class), mock(DisplacementRepository.class),
            mock(CurrentUser.class), mock(AuditService.class)
        );
    }

    @Test
    void createRejectsMismatchedDepartmentAndBranch() {
        CrudRequest.BagUpsert request = new CrudRequest.BagUpsert("bag", "barcode", 1L, 2L, null);
        Branch b1 = new Branch(); b1.setId(1L);
        Branch b2 = new Branch(); b2.setId(99L);
        Department d = new Department(); d.setId(2L); d.setName("Dept"); d.setBranch(b2);

        when(branchRepo.findById(1L)).thenReturn(Optional.of(b1));
        when(deptRepo.findById(2L)).thenReturn(Optional.of(d));

        ApiException ex = assertThrows(ApiException.class, () -> controller.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Department Dept does not belong to the selected branch", ex.getMessage());
    }

    @Test
    void updateRejectsMismatchedDepartmentAndBranch() {
        CrudRequest.BagUpsert request = new CrudRequest.BagUpsert("bag", "barcode", 1L, 2L, null);
        Branch b1 = new Branch(); b1.setId(1L);
        Branch b2 = new Branch(); b2.setId(99L);
        Department d = new Department(); d.setId(2L); d.setName("Dept"); d.setBranch(b2);
        com.inventorymanager.backend.domain.Bag bag = new com.inventorymanager.backend.domain.Bag();

        when(branchRepo.findById(1L)).thenReturn(Optional.of(b1));
        when(deptRepo.findById(2L)).thenReturn(Optional.of(d));
        when(bagRepo.findById(1L)).thenReturn(Optional.of(bag)); // Fix: mock the repository to return a bag so it doesn't throw 404

        ApiException ex = assertThrows(ApiException.class, () -> controller.update(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Department Dept does not belong to the selected branch", ex.getMessage());
    }
}
