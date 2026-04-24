package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.*;
import com.inventorymanager.backend.service.ItemRequestWorkflowService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/item-requests")
public class ItemRequestController {
    private final ItemRequestRepository repository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final ItemRequestWorkflowService workflowService;

    public ItemRequestController(
            ItemRequestRepository repository,
            UserRepository userRepository,
            ItemRepository itemRepository,
            CategoryRepository categoryRepository,
            DepartmentRepository departmentRepository,
            CurrentUser currentUser,
            AuditService auditService,
            ItemRequestWorkflowService workflowService
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_item_request')")
    public PageResponse<ItemRequest> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_item_request')")
    public ItemRequest get(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_item_request')")
    public ItemRequest create(@Valid @RequestBody CrudRequest.ItemRequestUpsert request) {
        User actor = actor();
        ItemRequest entity = new ItemRequest();
        apply(request, entity);
        entity.setRequestedBy(actor);
        entity.setRequestedAt(OffsetDateTime.now());
        entity.setStatus(ItemRequestStatus.DRAFT);
        ItemRequest saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_item_request')")
    public ItemRequest update(@PathVariable Long id, @Valid @RequestBody CrudRequest.ItemRequestUpsert request, Authentication authentication) {
        ItemRequest entity = find(id);
        assertOwnerOrAdmin(entity, authentication);
        if (entity.getStatus() != ItemRequestStatus.DRAFT && entity.getStatus() != ItemRequestStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft or rejected requests can be edited");
        }
        apply(request, entity);
        ItemRequest saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('submit_item_request')")
    public ItemRequest submit(@PathVariable Long id, Authentication authentication) {
        ItemRequest entity = find(id);
        assertOwnerOrAdmin(entity, authentication);
        if (entity.getStatus() != ItemRequestStatus.DRAFT && entity.getStatus() != ItemRequestStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft or rejected requests can be submitted");
        }
        entity.setStatus(ItemRequestStatus.SUBMITTED);
        ItemRequest saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('review_item_request')")
    public ItemRequest review(@PathVariable Long id, @Valid @RequestBody CrudRequest.ItemRequestReview review) {
        ItemRequest entity = find(id);
        if (entity.getStatus() != ItemRequestStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only submitted requests can be reviewed");
        }

        String decision = review.decision().trim().toLowerCase();
        if (!decision.equals("approve") && !decision.equals("reject")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Decision must be approve or reject");
        }

        entity.setReviewedBy(actor());
        entity.setReviewedAt(OffsetDateTime.now());
        entity.setReviewComment(review.comment());
        entity.setStatus(decision.equals("approve") ? ItemRequestStatus.APPROVED : ItemRequestStatus.REJECTED);
        ItemRequest saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('execute_item_request')")
    public ItemRequest execute(@PathVariable Long id) {
        ItemRequest entity = find(id);
        entity.setExecutedBy(actor());
        ItemRequest executed = workflowService.execute(entity, currentUser.id());
        ItemRequest saved = repository.save(executed);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    private ItemRequest find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item request not found"));
    }

    private User actor() {
        return userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void assertOwnerOrAdmin(ItemRequest request, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return;
        }
        if (request.getRequestedBy() == null || request.getRequestedBy().getId() == null || !request.getRequestedBy().getId().equals(currentUser.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only owner or admin can modify this request");
        }
    }

    private void apply(CrudRequest.ItemRequestUpsert request, ItemRequest entity) {
        entity.setRequestType(request.requestType());
        entity.setTitle(request.title());
        entity.setJustification(request.justification());
        List<ItemRequestEntry> entries = new ArrayList<>();
        for (CrudRequest.ItemRequestEntryUpsert entryUpsert : request.entries()) {
            ItemRequestEntry entry = new ItemRequestEntry();
            if (entryUpsert.itemId() != null) {
                Item item = itemRepository.findById(entryUpsert.itemId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Item not found: " + entryUpsert.itemId()));
                entry.setItem(item);
            }
            if (entryUpsert.requestedCategoryId() != null) {
                Category category = categoryRepository.findById(entryUpsert.requestedCategoryId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found: " + entryUpsert.requestedCategoryId()));
                entry.setRequestedCategory(category);
            }
            if (entryUpsert.sourceDepartmentId() != null) {
                Department source = departmentRepository.findById(entryUpsert.sourceDepartmentId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Source department not found: " + entryUpsert.sourceDepartmentId()));
                entry.setSourceDepartment(source);
            }
            if (entryUpsert.targetDepartmentId() != null) {
                Department target = departmentRepository.findById(entryUpsert.targetDepartmentId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Target department not found: " + entryUpsert.targetDepartmentId()));
                entry.setTargetDepartment(target);
            }
            entry.setRequestedItemName(entryUpsert.requestedItemName());
            entry.setRequestedQuantity(entryUpsert.requestedQuantity());
            entry.setRequestedUnit(entryUpsert.requestedUnit());
            entry.setObservations(entryUpsert.observations());
            entry.setCharacteristicsJson(entryUpsert.characteristicsJson());
            entries.add(entry);
        }
        entity.setEntries(entries);
    }
}
