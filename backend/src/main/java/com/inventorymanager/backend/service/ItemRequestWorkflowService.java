package com.inventorymanager.backend.service;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemRequestWorkflowService {
    private final ItemRepository itemRepository;
    private final AuditService auditService;

    public ItemRequestWorkflowService(ItemRepository itemRepository, AuditService auditService) {
        this.itemRepository = itemRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ItemRequest execute(ItemRequest request, Long actorUserId) {
        if (request.getStatus() != ItemRequestStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only approved requests can be executed");
        }

        for (ItemRequestEntry entry : request.getEntries()) {
            switch (request.getRequestType()) {
                case INBOUND -> executeInbound(entry, actorUserId);
                case MODIFICATION -> executeModification(entry, actorUserId);
                case TRANSFER -> executeTransfer(entry, actorUserId);
                case DISINCORPORATION -> executeDisincorporation(entry, actorUserId);
                case ADJUSTMENT -> executeAdjustment(entry, actorUserId);
            }
        }

        request.setStatus(ItemRequestStatus.EXECUTED);
        request.setExecutedAt(java.time.OffsetDateTime.now());
        return request;
    }

    private void executeInbound(ItemRequestEntry entry, Long actorUserId) {
        if (entry.getTargetDepartment() == null || entry.getRequestedItemName() == null || entry.getRequestedUnit() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Inbound entries require targetDepartment, requestedItemName, and requestedUnit");
        }
        Item item = new Item();
        item.setName(entry.getRequestedItemName());
        item.setQuantity(entry.getRequestedQuantity() == null ? 1 : Math.max(1, entry.getRequestedQuantity()));
        item.setUnit(entry.getRequestedUnit());
        item.setCategory(entry.getRequestedCategory());
        item.setDepartment(entry.getTargetDepartment());
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

    private void executeTransfer(ItemRequestEntry entry, Long actorUserId) {
        Item item = requireItem(entry);
        if (entry.getTargetDepartment() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Transfer entries require targetDepartment");
        }
        if (item.getDepartment() != null && item.getDepartment().getId() != null && entry.getTargetDepartment().getId() != null) {
            auditService.commitUnlink(actorUserId, "item_department", item.getId(), item.getDepartment().getId());
            auditService.commitLink(actorUserId, "item_department", item.getId(), entry.getTargetDepartment().getId());
        }
        item.setDepartment(entry.getTargetDepartment());
        Item saved = itemRepository.save(item);
        auditService.commitUpdate(actorUserId, saved);
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
