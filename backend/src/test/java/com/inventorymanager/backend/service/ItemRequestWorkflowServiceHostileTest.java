package com.inventorymanager.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.domain.ItemRequest;
import com.inventorymanager.backend.domain.ItemRequestStatus;
import com.inventorymanager.backend.domain.User;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import com.inventorymanager.backend.repository.ItemRequestRepository;
import com.inventorymanager.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.Optional;

public class ItemRequestWorkflowServiceHostileTest {

    private ItemRequestWorkflowService service;
    private ItemRequestRepository reqRepo;
    private UserRepository userRepo;

    @BeforeEach
    void setUp() {
        reqRepo = mock(ItemRequestRepository.class);
        userRepo = mock(UserRepository.class);
        service = new ItemRequestWorkflowService(
            mock(ItemRepository.class), reqRepo, userRepo,
            mock(DepartmentRepository.class), mock(AuditService.class)
        );
    }

    @Test
    void reviewRejectsIllegalStatusTransition() {
        ItemRequest req = new ItemRequest();
        req.setId(1L);
        req.setStatus(ItemRequestStatus.PENDING_REVIEW);
        
        when(reqRepo.findById(1L)).thenReturn(Optional.of(req));
        when(userRepo.findById(2L)).thenReturn(Optional.of(new User()));

        ApiException ex = assertThrows(ApiException.class, 
            () -> service.reviewRequest(1L, 2L, ItemRequestStatus.EXECUTED, "comment"));
        
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Invalid review transition to EXECUTED", ex.getMessage());
    }
}
