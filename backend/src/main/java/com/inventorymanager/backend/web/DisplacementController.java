package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Displacement;
import com.inventorymanager.backend.domain.DisplacementStatus;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.DisplacementRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/displacements")
@Tag(name = "Displacements", description = "Temporary borrowing and relocation of items")
public class DisplacementController {
    private final DisplacementRepository repository;
    private final BagRepository bagRepository;
    private final ItemRepository itemRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public DisplacementController(
            DisplacementRepository repository,
            BagRepository bagRepository,
            ItemRepository itemRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.bagRepository = bagRepository;
        this.itemRepository = itemRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_displacement')")
    @Operation(summary = "List all displacements", description = "Shows currently borrowed items and their expected return dates.")
    public PageResponse<Displacement> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_displacement')")
    @Operation(summary = "Get displacement by ID")
    public Displacement get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Displacement not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_displacement')")
    @Operation(summary = "Register temporary borrowing", description = "Creates a new displacement record. The item is marked as 'unofficially relocated'.")
    public Displacement create(@Valid @RequestBody CrudRequest.DisplacementUpsert request) {
        Displacement entity = new Displacement();
        mapRequestToEntity(request, entity);
        Displacement saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_displacement')")
    public Displacement update(@PathVariable Long id, @Valid @RequestBody CrudRequest.DisplacementUpsert request) {
        Displacement entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Displacement not found"));
        mapRequestToEntity(request, entity);
        Displacement saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('edit_displacement')")
    public Displacement resolve(@PathVariable Long id) {
        Displacement entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Displacement not found"));
        entity.setStatus(DisplacementStatus.RESOLVED);
        entity.setResolvedAt(java.time.OffsetDateTime.now());
        Displacement saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_displacement')")
    public void delete(@PathVariable Long id) {
        Displacement entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Displacement not found"));
        if (DisplacementStatus.ACTIVE.equals(entity.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete an active displacement. Resolve it first before deleting.");
        }
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }

    private void mapRequestToEntity(CrudRequest.DisplacementUpsert request, Displacement entity) {
        entity.setReason(request.reason());
        entity.setBorrowerName(request.borrowerName());
        entity.setExpectedReturnDate(request.expectedReturnDate());
        
        entity.setItem(itemRepository.findById(request.itemId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Item not found")));
        
        if (request.bagId() != null) {
            entity.setBag(bagRepository.findById(request.bagId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Bag not found")));
        } else {
            entity.setBag(null);
        }
    }
}
