package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.*;
import com.inventorymanager.backend.service.ItemRequestWorkflowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/item-requests")
public class ItemRequestController {
    private final ItemRequestRepository repository;
    private final BranchRepository branchRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final ItemRequestWorkflowService workflowService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public ItemRequestController(
            ItemRequestRepository repository,
            BranchRepository branchRepository,
            ItemRepository itemRepository,
            CategoryRepository categoryRepository,
            DepartmentRepository departmentRepository,
            ItemRequestWorkflowService workflowService,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.branchRepository = branchRepository;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.workflowService = workflowService;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_item_request')")
    public PageResponse<ItemRequest> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_item_request')")
    public ItemRequest get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_item_request')")
    @Transactional
    public ItemRequest create(@Valid @RequestBody CrudRequest.ItemRequestUpsert request) {
        if (request.entries() == null || request.entries().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Item request must contain at least one entry");
        }
        ItemRequest entity = new ItemRequest();
        mapRequestToEntity(request, entity);
        
        ItemRequest saved = workflowService.createRequest(entity, currentUser.id());
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_item_request')")
    @Transactional
    public ItemRequest update(@PathVariable Long id, @Valid @RequestBody CrudRequest.ItemRequestUpsert request) {
        if (request.entries() == null || request.entries().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Item request must contain at least one entry");
        }
        ItemRequest entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (entity.getStatus() != ItemRequestStatus.DRAFT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft requests can be edited");
        }
        mapRequestToEntity(request, entity);
        ItemRequest saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('submit_item_request')")
    @Transactional
    public ItemRequest submit(@PathVariable Long id) {
        ItemRequest saved = workflowService.submitRequest(id);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('review_item_request')")
    @Transactional
    public ItemRequest review(@PathVariable Long id, @Valid @RequestBody CrudRequest.ItemRequestReview review) {
        if (review == null || review.decision() == null || review.decision().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Decision is required");
        }
        
        ItemRequestStatus nextStatus;
        if (review.decision().equalsIgnoreCase("approve")) {
            nextStatus = ItemRequestStatus.APPROVED;
        } else if (review.decision().equalsIgnoreCase("reject")) {
            nextStatus = ItemRequestStatus.REJECTED;
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid decision: " + review.decision() + ". Must be 'approve' or 'reject'.");
        }
        
        ItemRequest saved = workflowService.reviewRequest(id, currentUser.id(), nextStatus, review.comment());
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('execute_item_request')")
    @Transactional
    public ItemRequest execute(@PathVariable Long id) {
        ItemRequest saved = workflowService.executeRequest(id, currentUser.id());
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    private void mapRequestToEntity(CrudRequest.ItemRequestUpsert request, ItemRequest entity) {
        entity.setRequestType(request.requestType());
        entity.setTitle(request.title());
        entity.setJustification(request.justification());
        
        if (request.targetBranchId() != null) {
            entity.setTargetBranch(branchRepository.findById(request.targetBranchId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Target branch not found")));
        }

        List<ItemRequestEntry> entries = new ArrayList<>();
        for (CrudRequest.ItemRequestEntryUpsert entryReq : request.entries()) {
            ItemRequestEntry entry = new ItemRequestEntry();
            
            if (entryReq.itemId() != null) {
                entry.setItem(itemRepository.findById(entryReq.itemId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Item not found: " + entryReq.itemId())));
            }
            
            entry.setRequestedItemName(entryReq.requestedItemName());
            entry.setRequestedQuantity(entryReq.requestedQuantity());
            entry.setRequestedUnit(entryReq.requestedUnit());
            
            if (entryReq.requestedCategoryId() != null) {
                entry.setRequestedCategory(categoryRepository.findById(entryReq.requestedCategoryId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found: " + entryReq.requestedCategoryId())));
            }
            
            if (entryReq.sourceDepartmentId() != null) {
                entry.setSourceDepartment(departmentRepository.findById(entryReq.sourceDepartmentId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Source department not found: " + entryReq.sourceDepartmentId())));
            }
            
            if (entryReq.targetDepartmentId() != null) {
                entry.setTargetDepartment(departmentRepository.findById(entryReq.targetDepartmentId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Target department not found: " + entryReq.targetDepartmentId())));
            }
            
            entry.setObservations(entryReq.observations());
            entry.setCharacteristicsJson(entryReq.characteristicsJson());
            entries.add(entry);
        }
        entity.setEntries(entries);
    }
}
