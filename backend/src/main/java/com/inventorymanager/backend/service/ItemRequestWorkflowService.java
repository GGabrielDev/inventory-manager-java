package com.inventorymanager.backend.service;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import com.inventorymanager.backend.repository.ItemRequestRepository;
import com.inventorymanager.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemRequestWorkflowService {
    private final ItemRepository itemRepository;
    private final ItemRequestRepository repository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public ItemRequestWorkflowService(
            ItemRepository itemRepository, 
            ItemRequestRepository repository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            AuditService auditService
    ) {
        this.itemRepository = itemRepository;
        this.repository = repository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ItemRequest createRequest(ItemRequest entity, Long requestedById) {
        User user = userRepository.findById(requestedById)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        entity.setRequestedBy(user);
        entity.setStatus(ItemRequestStatus.DRAFT);
        return repository.save(entity);
    }

    @Transactional
    public ItemRequest submitRequest(Long requestId) {
        ItemRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() != ItemRequestStatus.DRAFT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft requests can be submitted");
        }
        request.setStatus(ItemRequestStatus.PENDING_REVIEW);
        return repository.save(request);
    }

    @Transactional
    public ItemRequest reviewRequest(Long requestId, Long reviewerId, ItemRequestStatus nextStatus, String comment) {
        ItemRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reviewer not found"));
        
        if (request.getStatus() != ItemRequestStatus.PENDING_REVIEW) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending requests can be reviewed");
        }

        request.setStatus(nextStatus);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(java.time.OffsetDateTime.now());
        request.setReviewComment(comment);
        return repository.save(request);
    }

    @Transactional
    public ItemRequest executeRequest(Long requestId, Long actorUserId) {
        ItemRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Actor not found"));

        if (request.getStatus() != ItemRequestStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only approved requests can be executed");
        }

        for (ItemRequestEntry entry : request.getEntries()) {
            switch (request.getRequestType()) {
                case INBOUND -> executeInbound(request, entry, actorUserId);
                case MODIFICATION -> executeModification(entry, actorUserId);
                case TRANSFER -> executeTransfer(request, entry, actorUserId);
                case DISINCORPORATION -> executeDisincorporation(entry, actorUserId);
                case ADJUSTMENT -> executeAdjustment(entry, actorUserId);
            }
        }

        request.setStatus(ItemRequestStatus.EXECUTED);
        request.setExecutedBy(actor);
        request.setExecutedAt(java.time.OffsetDateTime.now());
        return repository.save(request);
    }

    private void executeInbound(ItemRequest request, ItemRequestEntry entry, Long actorUserId) {
        if (entry.getRequestedItemName() == null || entry.getRequestedUnit() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Inbound entries require requestedItemName and requestedUnit");
        }
        
        Branch targetBranch = request.getTargetBranch();
        if (targetBranch == null) {
            // Default to requestedBy user's branch if not specified
            targetBranch = request.getRequestedBy().getBranch();
        }
        if (targetBranch == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Target branch must be specified for inbound requests");
        }

        Department targetDept = entry.getTargetDepartment();
        if (targetDept == null) {
            // Default to "Inbound" department of target branch
            targetDept = departmentRepository.findByNameAndBranch_Id("Inbound", targetBranch.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Inbound department not found in target branch"));
        }

        Item item = new Item();
        item.setName(entry.getRequestedItemName());
        item.setQuantity(entry.getRequestedQuantity() == null ? 1 : Math.max(1, entry.getRequestedQuantity()));
        item.setUnit(entry.getRequestedUnit());
        item.setCategory(entry.getRequestedCategory());
        item.setBranch(targetBranch);
        item.setDepartment(targetDept);
        item.setObservations(entry.getObservations());
        item.setCharacteristicsJson(entry.getCharacteristicsJson());
        Item saved = itemRepository.save(item);
        auditService.commitCreate(actorUserId, saved);
    }

    private void executeModification(ItemRequestEntry entry, Long actorUserId) {
        Item item = requireItem(entry);
        if (entry.getRequestedItemName() != null && !entry.getRequestedItemName().isBlank()) {
            item.setName(entry.getRequestedItemName());
        }
        if (entry.getRequestedQuantity() != null) {
            item.setQuantity(Math.max(1, entry.getRequestedQuantity()));
        }
        if (entry.getRequestedUnit() != null) {
            item.setUnit(entry.getRequestedUnit());
        }
        if (entry.getRequestedCategory() != null) {
            item.setCategory(entry.getRequestedCategory());
        }
        if (entry.getTargetDepartment() != null) {
            item.setDepartment(entry.getTargetDepartment());
        }
        if (entry.getObservations() != null) {
            item.setObservations(entry.getObservations());
        }
        if (entry.getCharacteristicsJson() != null) {
            item.setCharacteristicsJson(entry.getCharacteristicsJson());
        }
        Item saved = itemRepository.save(item);
        auditService.commitUpdate(actorUserId, saved);
    }

    private void executeTransfer(ItemRequest request, ItemRequestEntry entry, Long actorUserId) {
        Item item = requireItem(entry);
        
        Branch targetBranch = request.getTargetBranch();
        Department targetDept = entry.getTargetDepartment();

        if (targetBranch != null && !targetBranch.getId().equals(item.getBranch().getId())) {
            // INTER-BRANCH TRANSFER
            item.setBranch(targetBranch);
            // Default to Inbound department of target branch
            targetDept = departmentRepository.findByNameAndBranch_Id("Inbound", targetBranch.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Inbound department not found in target branch"));
            item.setDepartment(targetDept);
        } else if (targetDept != null) {
            // INTRA-BRANCH TRANSFER
            item.setDepartment(targetDept);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Transfer requires either a target branch or a target department");
        }

        Item saved = itemRepository.save(item);
        auditService.commitUpdate(actorUserId, saved);

        // Audit relationship changes specifically
        auditService.commitLink(actorUserId, "item_department", saved.getId(), saved.getDepartment().getId());
        auditService.commitLink(actorUserId, "item_branch", saved.getId(), saved.getBranch().getId());
    }

    private void executeDisincorporation(ItemRequestEntry entry, Long actorUserId) {
        Item item = requireItem(entry);
        int amount = entry.getRequestedQuantity() == null ? item.getQuantity() : Math.max(1, entry.getRequestedQuantity());
        int next = item.getQuantity() - amount;
        if (next <= 0) {
            itemRepository.delete(item);
            auditService.commitDelete(actorUserId, item);
            return;
        }
        item.setQuantity(next);
        Item saved = itemRepository.save(item);
        auditService.commitUpdate(actorUserId, saved);
    }

    private void executeAdjustment(ItemRequestEntry entry, Long actorUserId) {
        Item item = requireItem(entry);
        if (entry.getRequestedQuantity() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Adjustment entries require requestedQuantity as signed delta");
        }
        int next = item.getQuantity() + entry.getRequestedQuantity();
        if (next <= 0) {
            itemRepository.delete(item);
            auditService.commitDelete(actorUserId, item);
            return;
        }
        item.setQuantity(next);
        Item saved = itemRepository.save(item);
        auditService.commitUpdate(actorUserId, saved);
    }

    private Item requireItem(ItemRequestEntry entry) {
        if (entry.getItem() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Entry requires an existing item reference");
        }
        return entry.getItem();
    }
}
