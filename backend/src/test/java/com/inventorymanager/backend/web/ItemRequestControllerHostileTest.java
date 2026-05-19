package com.inventorymanager.backend.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.domain.ItemRequestType;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.CategoryRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import com.inventorymanager.backend.repository.ItemRequestRepository;
import com.inventorymanager.backend.service.ItemRequestWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.List;

public class ItemRequestControllerHostileTest {

    private ItemRequestController controller;
    private ItemRequestWorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = mock(ItemRequestWorkflowService.class);
        controller = new ItemRequestController(
            mock(ItemRequestRepository.class), mock(BranchRepository.class),
            mock(ItemRepository.class), mock(CategoryRepository.class),
            mock(DepartmentRepository.class), workflowService,
            mock(CurrentUser.class), mock(AuditService.class)
        );
    }

    @Test
    void createRejectsEmptyEntries() {
        CrudRequest.ItemRequestUpsert request = new CrudRequest.ItemRequestUpsert(
            ItemRequestType.INBOUND, "title", "just", List.of(), null
        );
        ApiException ex = assertThrows(ApiException.class, () -> controller.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Item request must contain at least one entry", ex.getMessage());
    }

    @Test
    void updateRejectsEmptyEntries() {
        CrudRequest.ItemRequestUpsert request = new CrudRequest.ItemRequestUpsert(
            ItemRequestType.INBOUND, "title", "just", List.of(), null
        );
        ApiException ex = assertThrows(ApiException.class, () -> controller.update(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Item request must contain at least one entry", ex.getMessage());
    }

    @Test
    void reviewRejectsNullDecision() {
        CrudRequest.ItemRequestReview review = new CrudRequest.ItemRequestReview(null, "comment");
        ApiException ex = assertThrows(ApiException.class, () -> controller.review(1L, review));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Decision is required", ex.getMessage());
    }

    @Test
    void reviewRejectsInvalidDecision() {
        CrudRequest.ItemRequestReview review = new CrudRequest.ItemRequestReview("maybe", "comment");
        ApiException ex = assertThrows(ApiException.class, () -> controller.review(1L, review));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Invalid decision: maybe. Must be 'approve' or 'reject'.", ex.getMessage());
    }
}
